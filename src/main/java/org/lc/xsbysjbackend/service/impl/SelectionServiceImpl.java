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
import org.lc.xsbysjbackend.mapper.SelectionMapper;
import org.lc.xsbysjbackend.model.dto.selection.SelectionApplyRequest;
import org.lc.xsbysjbackend.model.dto.selection.SelectionQueryRequest;
import org.lc.xsbysjbackend.model.dto.selection.SelectionReviewRequest;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.model.enums.SelectionStatusEnum;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.model.vo.SelectionVO;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 选题服务实现。
 */
@Service
@RequiredArgsConstructor
public class SelectionServiceImpl extends ServiceImpl<SelectionMapper, Selection>
        implements SelectionService {

    private static final int MAX_AUDIT_COMMENT_LENGTH = 1000;

    private static final Set<String> ACTIVE_STATUSES = Set.of(
            SelectionStatusEnum.PENDING.name(),
            SelectionStatusEnum.APPROVED.name()
    );

    private final TopicService topicService;

    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long applySelection(SelectionApplyRequest selectionApplyRequest) {
        User student = getCurrentStudent();
        if (selectionApplyRequest == null || selectionApplyRequest.getTopicId() == null
                || selectionApplyRequest.getTopicId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课题主键不能为空");
        }

        Topic topic = lockTopic(selectionApplyRequest.getTopicId());
        if (!TopicStatusEnum.APPROVED.name().equals(topic.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该课题尚未审核通过");
        }

        Selection activeSelection = lambdaQuery()
                .eq(Selection::getStudentid, student.getId())
                .in(Selection::getStatus, ACTIVE_STATUSES)
                .last("FOR UPDATE")
                .one();
        if (activeSelection != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已有待审核或已通过的选题");
        }
        if (countApprovedSelections(topic.getId()) >= topic.getMaxstudents()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "课题名额已满");
        }

        Selection historicalSelection = lambdaQuery()
                .eq(Selection::getStudentid, student.getId())
                .eq(Selection::getTopicid, topic.getId())
                .last("FOR UPDATE")
                .one();
        Date now = new Date();
        if (historicalSelection != null) {
            reactivateSelection(historicalSelection.getId(), now);
            return historicalSelection.getId();
        }

        Selection selection = new Selection();
        selection.setStudentid(student.getId());
        selection.setTopicid(topic.getId());
        selection.setSelectiontime(now);
        selection.setStatus(SelectionStatusEnum.PENDING.name());
        if (!save(selection)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交选题申请失败");
        }
        return selection.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelSelection(long id) {
        User student = getCurrentStudent();
        Selection selection = lockSelection(id);
        if (!student.getId().equals(selection.getStudentid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权取消其他学生的选题");
        }
        if (!SelectionStatusEnum.PENDING.name().equals(selection.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有待审核选题可以取消");
        }
        boolean updated = lambdaUpdate()
                .eq(Selection::getId, selection.getId())
                .set(Selection::getStatus, SelectionStatusEnum.CANCELED.name())
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "取消选题失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewSelection(SelectionReviewRequest selectionReviewRequest) {
        User teacher = getCurrentTeacher();
        if (selectionReviewRequest == null || selectionReviewRequest.getId() == null
                || selectionReviewRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选题主键不能为空");
        }
        if (selectionReviewRequest.getApproved() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核结果不能为空");
        }

        Selection currentSelection = getSelectionOrThrow(selectionReviewRequest.getId());
        Topic topic = lockTopic(currentSelection.getTopicid());
        Selection selection = lockSelection(selectionReviewRequest.getId());
        if (!teacher.getId().equals(topic.getTeacherid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权审核其他教师课题的选题");
        }
        if (!SelectionStatusEnum.PENDING.name().equals(selection.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有待审核选题可以审核");
        }

        String auditComment = normalizeNullable(selectionReviewRequest.getAuditComment());
        validateMaxLength(auditComment, MAX_AUDIT_COMMENT_LENGTH, "审核意见不能超过1000个字符");
        if (!selectionReviewRequest.getApproved() && StrUtil.isBlank(auditComment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "驳回选题时必须填写审核意见");
        }
        if (selectionReviewRequest.getApproved()) {
            if (!TopicStatusEnum.APPROVED.name().equals(topic.getStatus())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "课题未审核通过，不能通过选题");
            }
            if (countApprovedSelections(topic.getId()) >= topic.getMaxstudents()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "课题名额已满");
            }
        }

        String status = selectionReviewRequest.getApproved()
                ? SelectionStatusEnum.APPROVED.name()
                : SelectionStatusEnum.REJECTED.name();
        updateSelectionReview(selection.getId(), status, teacher.getId(), auditComment, new Date());
        return true;
    }

    @Override
    public SelectionVO getSelectionVoById(long id) {
        Selection selection = getSelectionOrThrow(id);
        Topic topic = getTopicOrThrow(selection.getTopicid());
        User student = userService.getById(selection.getStudentid());
        User teacher = userService.getById(topic.getTeacherid());
        checkViewPermission(selection, topic, userService.getLoginUser());
        return SelectionVO.fromSelection(selection, topic, student, teacher);
    }

    @Override
    public IPage<SelectionVO> listSelectionVoByPage(SelectionQueryRequest selectionQueryRequest) {
        if (selectionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        if (selectionQueryRequest.getCurrent() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (selectionQueryRequest.getPageSize() <= 0 || selectionQueryRequest.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1到50之间");
        }
        if (StrUtil.isNotBlank(selectionQueryRequest.getStatus())
                && SelectionStatusEnum.getByValue(selectionQueryRequest.getStatus()) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选题状态不合法");
        }

        User currentUser = userService.getLoginUser();
        LambdaQueryWrapper<Selection> queryWrapper = new LambdaQueryWrapper<>();
        if (!applyRoleScope(queryWrapper, currentUser)) {
            return emptyPage(selectionQueryRequest);
        }
        queryWrapper.eq(selectionQueryRequest.getId() != null,
                Selection::getId, selectionQueryRequest.getId());
        queryWrapper.eq(selectionQueryRequest.getTopicId() != null,
                Selection::getTopicid, selectionQueryRequest.getTopicId());
        if (!UserConstant.STUDENT_ROLE.equals(currentUser.getRole())) {
            queryWrapper.eq(selectionQueryRequest.getStudentId() != null,
                    Selection::getStudentid, selectionQueryRequest.getStudentId());
        }
        queryWrapper.eq(StrUtil.isNotBlank(selectionQueryRequest.getStatus()),
                Selection::getStatus, selectionQueryRequest.getStatus());

        List<Long> titleTopicIds = getTopicIdsByTitle(selectionQueryRequest.getTopicTitle());
        if (titleTopicIds != null) {
            if (titleTopicIds.isEmpty()) {
                return emptyPage(selectionQueryRequest);
            }
            queryWrapper.in(Selection::getTopicid, titleTopicIds);
        }
        applySelectionSort(
                queryWrapper,
                selectionQueryRequest.getSortField(),
                selectionQueryRequest.getSortOrder()
        );

        Page<Selection> selectionPage = page(
                new Page<>(selectionQueryRequest.getCurrent(), selectionQueryRequest.getPageSize()),
                queryWrapper
        );
        Page<SelectionVO> voPage = new Page<>(
                selectionPage.getCurrent(),
                selectionPage.getSize(),
                selectionPage.getTotal()
        );
        voPage.setRecords(toSelectionVoList(selectionPage.getRecords()));
        return voPage;
    }

    private boolean applyRoleScope(LambdaQueryWrapper<Selection> queryWrapper, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            return true;
        }
        if (UserConstant.STUDENT_ROLE.equals(role)) {
            queryWrapper.eq(Selection::getStudentid, currentUser.getId());
            return true;
        }
        if (UserConstant.TEACHER_ROLE.equals(role)) {
            List<Long> topicIds = topicService.lambdaQuery()
                    .eq(Topic::getTeacherid, currentUser.getId())
                    .select(Topic::getId)
                    .list()
                    .stream()
                    .map(Topic::getId)
                    .toList();
            if (topicIds.isEmpty()) {
                return false;
            }
            queryWrapper.in(Selection::getTopicid, topicIds);
            return true;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查询选题");
    }

    private void checkViewPermission(Selection selection, Topic topic, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)
                || (UserConstant.STUDENT_ROLE.equals(role)
                && currentUser.getId().equals(selection.getStudentid()))
                || (UserConstant.TEACHER_ROLE.equals(role)
                && currentUser.getId().equals(topic.getTeacherid()))) {
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该选题");
    }

    private List<SelectionVO> toSelectionVoList(List<Selection> selections) {
        if (selections.isEmpty()) {
            return List.of();
        }
        Set<Long> topicIds = selections.stream()
                .map(Selection::getTopicid)
                .collect(Collectors.toSet());
        Set<Long> studentIds = selections.stream()
                .map(Selection::getStudentid)
                .collect(Collectors.toSet());
        Map<Long, Topic> topicMap = topicService.listByIds(topicIds).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
        Set<Long> teacherIds = topicMap.values().stream()
                .map(Topic::getTeacherid)
                .collect(Collectors.toSet());
        Set<Long> userIds = Stream.concat(studentIds.stream(), teacherIds.stream())
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return selections.stream()
                .map(selection -> {
                    Topic topic = topicMap.get(selection.getTopicid());
                    User student = userMap.get(selection.getStudentid());
                    User teacher = topic == null ? null : userMap.get(topic.getTeacherid());
                    return SelectionVO.fromSelection(selection, topic, student, teacher);
                })
                .toList();
    }

    private List<Long> getTopicIdsByTitle(String topicTitle) {
        if (StrUtil.isBlank(topicTitle)) {
            return null;
        }
        return topicService.lambdaQuery()
                .like(Topic::getTitle, topicTitle.trim())
                .select(Topic::getId)
                .list()
                .stream()
                .map(Topic::getId)
                .toList();
    }

    private void updateSelectionReview(long id, String status, Long reviewerId,
                                       String auditComment, Date reviewTime) {
        boolean updated = lambdaUpdate()
                .eq(Selection::getId, id)
                .set(Selection::getStatus, status)
                .set(Selection::getReviewerid, reviewerId)
                .set(Selection::getAuditcomment, auditComment)
                .set(Selection::getReviewtime, reviewTime)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新选题状态失败");
        }
    }

    private void reactivateSelection(long id, Date selectionTime) {
        boolean updated = lambdaUpdate()
                .eq(Selection::getId, id)
                .set(Selection::getStatus, SelectionStatusEnum.PENDING.name())
                .set(Selection::getSelectiontime, selectionTime)
                .set(Selection::getReviewerid, null)
                .set(Selection::getAuditcomment, null)
                .set(Selection::getReviewtime, null)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "重新提交选题申请失败");
        }
    }

    private Selection lockSelection(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选题主键不合法");
        }
        Selection selection = lambdaQuery()
                .eq(Selection::getId, id)
                .last("FOR UPDATE")
                .one();
        if (selection == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "选题记录不存在");
        }
        return selection;
    }

    private Selection getSelectionOrThrow(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选题主键不合法");
        }
        Selection selection = getById(id);
        if (selection == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "选题记录不存在");
        }
        return selection;
    }

    private Topic lockTopic(long id) {
        Topic topic = topicService.getOne(new LambdaQueryWrapper<Topic>()
                .eq(Topic::getId, id)
                .last("FOR UPDATE"));
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课题不存在");
        }
        return topic;
    }

    private Topic getTopicOrThrow(long id) {
        Topic topic = topicService.getById(id);
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课题不存在");
        }
        return topic;
    }

    private long countApprovedSelections(long topicId) {
        return lambdaQuery()
                .eq(Selection::getTopicid, topicId)
                .eq(Selection::getStatus, SelectionStatusEnum.APPROVED.name())
                .count();
    }

    private User getCurrentStudent() {
        User user = userService.getLoginUser();
        if (!UserConstant.STUDENT_ROLE.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可以执行该操作");
        }
        return user;
    }

    private User getCurrentTeacher() {
        User user = userService.getLoginUser();
        if (!UserConstant.TEACHER_ROLE.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可以执行该操作");
        }
        return user;
    }

    private void validateMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, message);
        }
    }

    private static String normalizeNullable(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private static IPage<SelectionVO> emptyPage(SelectionQueryRequest request) {
        Page<SelectionVO> page = new Page<>(request.getCurrent(), request.getPageSize(), 0);
        page.setRecords(Collections.emptyList());
        return page;
    }

    private static void applySelectionSort(LambdaQueryWrapper<Selection> queryWrapper,
                                           String sortField, String sortOrder) {
        boolean ascending = "ascend".equalsIgnoreCase(sortOrder);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByDesc(Selection::getUpdatetime);
            return;
        }
        switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, Selection::getId);
            case "selectionTime" -> queryWrapper.orderBy(
                    true, ascending, Selection::getSelectiontime
            );
            case "status" -> queryWrapper.orderBy(true, ascending, Selection::getStatus);
            case "createTime" -> queryWrapper.orderBy(
                    true, ascending, Selection::getCreatetime
            );
            case "updateTime" -> queryWrapper.orderBy(
                    true, ascending, Selection::getUpdatetime
            );
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序字段不合法");
        }
    }

}
