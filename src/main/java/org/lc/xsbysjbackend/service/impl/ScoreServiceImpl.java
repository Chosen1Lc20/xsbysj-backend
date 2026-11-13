package org.lc.xsbysjbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.exception.BusinessException;
import org.lc.xsbysjbackend.exception.ErrorCode;
import org.lc.xsbysjbackend.mapper.DefenseGroupStudentMapper;
import org.lc.xsbysjbackend.mapper.ScoreMapper;
import org.lc.xsbysjbackend.model.dto.score.ScoreQueryRequest;
import org.lc.xsbysjbackend.model.dto.score.ScoreSaveRequest;
import org.lc.xsbysjbackend.model.entity.DefenseGroupStudent;
import org.lc.xsbysjbackend.model.entity.Score;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.model.enums.SelectionStatusEnum;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.model.vo.ScoreVO;
import org.lc.xsbysjbackend.service.ScoreService;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author lianchao0921
* @description 针对表【score(成绩表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
@RequiredArgsConstructor
public class ScoreServiceImpl extends ServiceImpl<ScoreMapper, Score>
    implements ScoreService {

    private static final BigDecimal PROCESS_WEIGHT = new BigDecimal("0.4");

    private static final BigDecimal DEFENSE_WEIGHT = new BigDecimal("0.6");

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private static final int MAX_COMMENT_LENGTH = 1000;

    private final UserService userService;

    private final SelectionService selectionService;

    private final TopicService topicService;

    private final DefenseGroupStudentMapper defenseGroupStudentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoreVO saveScore(ScoreSaveRequest scoreSaveRequest) {
        if (scoreSaveRequest == null || scoreSaveRequest.getStudentId() == null
                || scoreSaveRequest.getStudentId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学生主键不能为空");
        }
        User teacher = getCurrentTeacher();
        BigDecimal processScore = validateScore(
                scoreSaveRequest.getProcessScore(),
                "过程成绩"
        );
        BigDecimal defenseScore = validateScore(
                scoreSaveRequest.getDefenseScore(),
                "答辩成绩"
        );
        String teacherComment = normalizeNullable(scoreSaveRequest.getTeacherComment());
        validateCommentLength(teacherComment);

        User student = userService.getById(scoreSaveRequest.getStudentId());
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学生不存在");
        }
        if (!UserConstant.STUDENT_ROLE.equals(student.getRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "成绩所属用户必须是学生");
        }
        Selection selection = getApprovedSelection(student.getId());
        Topic topic = getTopicOrThrow(selection.getTopicid());
        if (!TopicStatusEnum.APPROVED.name().equals(topic.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "学生选题对应课题尚未审核通过");
        }
        if (!teacher.getId().equals(topic.getTeacherid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能录入本人课题学生的成绩");
        }
        long defenseAssignmentCount = defenseGroupStudentMapper.selectCount(
                new LambdaQueryWrapper<DefenseGroupStudent>()
                        .eq(DefenseGroupStudent::getStudentid, student.getId())
                        .eq(DefenseGroupStudent::getSelectionid, selection.getId())
        );
        if (defenseAssignmentCount == 0) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR,
                    "学生尚未分配答辩组，不能录入成绩"
            );
        }

        BigDecimal totalScore = processScore.multiply(PROCESS_WEIGHT)
                .add(defenseScore.multiply(DEFENSE_WEIGHT))
                .setScale(1, RoundingMode.HALF_UP);
        Date now = new Date();
        Score existingScore = lambdaQuery()
                .eq(Score::getStudentid, student.getId())
                .one();
        if (existingScore == null) {
            Score score = new Score();
            score.setStudentid(student.getId());
            score.setSelectionid(selection.getId());
            score.setTopicid(topic.getId());
            score.setTeacherid(teacher.getId());
            score.setProcessscore(processScore);
            score.setDefensescore(defenseScore);
            score.setTotalscore(totalScore);
            score.setTeachercomment(teacherComment);
            score.setEntrytime(now);
            try {
                if (!save(score)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "录入成绩失败");
                }
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该学生成绩已存在");
            }
            return toScoreVoList(List.of(score)).get(0);
        }

        boolean updated = lambdaUpdate()
                .eq(Score::getId, existingScore.getId())
                .set(Score::getSelectionid, selection.getId())
                .set(Score::getTopicid, topic.getId())
                .set(Score::getTeacherid, teacher.getId())
                .set(Score::getProcessscore, processScore)
                .set(Score::getDefensescore, defenseScore)
                .set(Score::getTotalscore, totalScore)
                .set(Score::getTeachercomment, teacherComment)
                .set(Score::getEntrytime, now)
                .set(Score::getUpdatetime, now)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新成绩失败");
        }
        return toScoreVoList(List.of(getById(existingScore.getId()))).get(0);
    }

    @Override
    public ScoreVO getScoreVoById(long id) {
        Score score = getScoreOrThrow(id);
        checkViewPermission(score, userService.getLoginUser());
        return toScoreVoList(List.of(score)).get(0);
    }

    @Override
    public ScoreVO getMyScore() {
        User student = getCurrentStudent();
        Score score = lambdaQuery()
                .eq(Score::getStudentid, student.getId())
                .one();
        return score == null ? null : toScoreVoList(List.of(score)).get(0);
    }

    @Override
    public IPage<ScoreVO> listScoreVoByPage(ScoreQueryRequest scoreQueryRequest) {
        validatePageRequest(scoreQueryRequest);
        User currentUser = userService.getLoginUser();
        LambdaQueryWrapper<Score> queryWrapper = new LambdaQueryWrapper<>();
        applyRoleScope(queryWrapper, currentUser);

        queryWrapper.eq(scoreQueryRequest.getId() != null,
                Score::getId, scoreQueryRequest.getId());
        queryWrapper.eq(scoreQueryRequest.getSelectionId() != null,
                Score::getSelectionid, scoreQueryRequest.getSelectionId());
        queryWrapper.eq(scoreQueryRequest.getTopicId() != null,
                Score::getTopicid, scoreQueryRequest.getTopicId());
        queryWrapper.eq(scoreQueryRequest.getStudentId() != null,
                Score::getStudentid, scoreQueryRequest.getStudentId());
        queryWrapper.eq(scoreQueryRequest.getTeacherId() != null,
                Score::getTeacherid, scoreQueryRequest.getTeacherId());
        applyScoreSort(
                queryWrapper,
                scoreQueryRequest.getSortField(),
                scoreQueryRequest.getSortOrder()
        );

        Page<Score> scorePage = page(
                new Page<>(scoreQueryRequest.getCurrent(), scoreQueryRequest.getPageSize()),
                queryWrapper
        );
        Page<ScoreVO> voPage = new Page<>(
                scorePage.getCurrent(),
                scorePage.getSize(),
                scorePage.getTotal()
        );
        voPage.setRecords(toScoreVoList(scorePage.getRecords()));
        return voPage;
    }

    private void applyRoleScope(LambdaQueryWrapper<Score> queryWrapper, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            return;
        }
        if (UserConstant.TEACHER_ROLE.equals(role)) {
            queryWrapper.eq(Score::getTeacherid, currentUser.getId());
            return;
        }
        if (UserConstant.STUDENT_ROLE.equals(role)) {
            queryWrapper.eq(Score::getStudentid, currentUser.getId());
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查询成绩");
    }

    private void checkViewPermission(Score score, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)
                || (UserConstant.TEACHER_ROLE.equals(role)
                && currentUser.getId().equals(score.getTeacherid()))
                || (UserConstant.STUDENT_ROLE.equals(role)
                && currentUser.getId().equals(score.getStudentid()))) {
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该成绩");
    }

    private Selection getApprovedSelection(long studentId) {
        Selection selection = selectionService.lambdaQuery()
                .eq(Selection::getStudentid, studentId)
                .eq(Selection::getStatus, SelectionStatusEnum.APPROVED.name())
                .orderByDesc(Selection::getSelectiontime)
                .last("LIMIT 1")
                .one();
        if (selection == null) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR,
                    "学生没有审核通过的选题，不能录入成绩"
            );
        }
        return selection;
    }

    private Topic getTopicOrThrow(long id) {
        Topic topic = topicService.getById(id);
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课题不存在");
        }
        return topic;
    }

    private Score getScoreOrThrow(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "成绩主键不合法");
        }
        Score score = getById(id);
        if (score == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "成绩不存在");
        }
        return score;
    }

    private User getCurrentTeacher() {
        User user = userService.getLoginUser();
        if (!UserConstant.TEACHER_ROLE.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅指导教师可以录入成绩");
        }
        return user;
    }

    private User getCurrentStudent() {
        User user = userService.getLoginUser();
        if (!UserConstant.STUDENT_ROLE.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可以查询本人成绩接口");
        }
        return user;
    }

    private BigDecimal validateScore(BigDecimal score, String fieldName) {
        if (score == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + "不能为空");
        }
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + "必须在0到100之间");
        }
        return score.setScale(1, RoundingMode.HALF_UP);
    }

    private void validateCommentLength(String teacherComment) {
        if (teacherComment != null && teacherComment.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师评语不能超过1000个字符");
        }
    }

    private void validatePageRequest(ScoreQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        if (request.getCurrent() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1到50之间");
        }
    }

    private List<ScoreVO> toScoreVoList(List<Score> scores) {
        if (scores.isEmpty()) {
            return List.of();
        }
        Set<Long> selectionIds = scores.stream()
                .map(Score::getSelectionid)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> topicIds = scores.stream()
                .map(Score::getTopicid)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Selection> selectionMap = selectionIds.isEmpty()
                ? Collections.emptyMap()
                : selectionService.listByIds(selectionIds).stream()
                .collect(Collectors.toMap(Selection::getId, Function.identity()));
        Map<Long, Topic> topicMap = topicIds.isEmpty()
                ? Collections.emptyMap()
                : topicService.listByIds(topicIds).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));

        Set<Long> userIds = new LinkedHashSet<>();
        scores.stream().map(Score::getStudentid).filter(id -> id != null).forEach(userIds::add);
        scores.stream().map(Score::getTeacherid).filter(id -> id != null).forEach(userIds::add);
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return scores.stream()
                .map(score -> ScoreVO.fromScore(
                        score,
                        selectionMap.get(score.getSelectionid()),
                        topicMap.get(score.getTopicid()),
                        userMap.get(score.getStudentid()),
                        userMap.get(score.getTeacherid())
                ))
                .toList();
    }

    private static String normalizeNullable(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private static void applyScoreSort(LambdaQueryWrapper<Score> queryWrapper,
                                       String sortField, String sortOrder) {
        boolean ascending = "ascend".equalsIgnoreCase(sortOrder);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByDesc(Score::getEntrytime);
            return;
        }
        switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, Score::getId);
            case "processScore" -> queryWrapper.orderBy(
                    true, ascending, Score::getProcessscore
            );
            case "defenseScore" -> queryWrapper.orderBy(
                    true, ascending, Score::getDefensescore
            );
            case "totalScore" -> queryWrapper.orderBy(
                    true, ascending, Score::getTotalscore
            );
            case "entryTime" -> queryWrapper.orderBy(
                    true, ascending, Score::getEntrytime
            );
            case "updateTime" -> queryWrapper.orderBy(
                    true, ascending, Score::getUpdatetime
            );
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序字段不合法");
        }
    }
}
