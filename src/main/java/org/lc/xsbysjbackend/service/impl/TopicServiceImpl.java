package org.lc.xsbysjbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.exception.BusinessException;
import org.lc.xsbysjbackend.exception.ErrorCode;
import org.lc.xsbysjbackend.mapper.TopicMapper;
import org.lc.xsbysjbackend.model.dto.topic.TopicAddRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicQueryRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicReviewRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicUpdateRequest;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.model.vo.TopicVO;
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

/**
 * 课题服务实现。
 */
@Service
@RequiredArgsConstructor
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic>
        implements TopicService {

    private static final int MAX_TITLE_LENGTH = 255;

    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    private static final int MAX_TYPE_LENGTH = 64;

    private static final int MAX_AUDIT_COMMENT_LENGTH = 1000;

    private static final int MAX_STUDENTS_LIMIT = 20;

    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTopic(TopicAddRequest topicAddRequest) {
        if (topicAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新增课题请求不能为空");
        }
        User teacher = getCurrentTeacher();

        String title = validateAndNormalizeTitle(topicAddRequest.getTitle());
        String description = validateAndNormalizeDescription(topicAddRequest.getDescription());
        String type = validateAndNormalizeType(topicAddRequest.getType());
        int maxStudents = validateMaxStudents(
                topicAddRequest.getMaxStudents() == null ? 1 : topicAddRequest.getMaxStudents()
        );

        Topic topic = new Topic();
        topic.setTeacherid(teacher.getId());
        topic.setTitle(title);
        topic.setDescription(description);
        topic.setType(type);
        topic.setMaxstudents(maxStudents);
        topic.setStatus(TopicStatusEnum.DRAFT.name());
        if (!save(topic)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增课题失败");
        }
        return topic.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTopic(TopicUpdateRequest topicUpdateRequest) {
        if (topicUpdateRequest == null || topicUpdateRequest.getId() == null
                || topicUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课题主键不能为空");
        }
        User teacher = getCurrentTeacher();
        Topic topic = getTopicOrThrow(topicUpdateRequest.getId());
        checkTeacherOwner(topic, teacher);
        checkEditable(topic);

        LambdaUpdateWrapper<Topic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Topic::getId, topic.getId());
        boolean hasUpdate = false;
        if (topicUpdateRequest.getTitle() != null) {
            updateWrapper.set(Topic::getTitle,
                    validateAndNormalizeTitle(topicUpdateRequest.getTitle()));
            hasUpdate = true;
        }
        if (topicUpdateRequest.getDescription() != null) {
            updateWrapper.set(Topic::getDescription,
                    validateAndNormalizeDescription(topicUpdateRequest.getDescription()));
            hasUpdate = true;
        }
        if (topicUpdateRequest.getType() != null) {
            updateWrapper.set(Topic::getType,
                    validateAndNormalizeType(topicUpdateRequest.getType()));
            hasUpdate = true;
        }
        if (topicUpdateRequest.getMaxStudents() != null) {
            updateWrapper.set(Topic::getMaxstudents,
                    validateMaxStudents(topicUpdateRequest.getMaxStudents()));
            hasUpdate = true;
        }
        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有需要更新的字段");
        }
        if (!update(updateWrapper)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新课题失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTopic(long id) {
        User teacher = getCurrentTeacher();
        Topic topic = getTopicOrThrow(id);
        checkTeacherOwner(topic, teacher);
        checkEditable(topic);
        if (!removeById(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除课题失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitTopicReview(long id) {
        User teacher = getCurrentTeacher();
        Topic topic = getTopicOrThrow(id);
        checkTeacherOwner(topic, teacher);
        checkEditable(topic);

        boolean updated = lambdaUpdate()
                .eq(Topic::getId, topic.getId())
                .set(Topic::getStatus, TopicStatusEnum.PENDING.name())
                .set(Topic::getReviewerid, null)
                .set(Topic::getAuditcomment, null)
                .set(Topic::getReviewtime, null)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交课题审核失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewTopic(TopicReviewRequest topicReviewRequest) {
        User admin = getCurrentAdmin();
        if (topicReviewRequest == null || topicReviewRequest.getId() == null
                || topicReviewRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课题主键不能为空");
        }
        if (topicReviewRequest.getApproved() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核结果不能为空");
        }

        Topic topic = getTopicOrThrow(topicReviewRequest.getId());
        if (!TopicStatusEnum.PENDING.name().equals(topic.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有待审核课题可以审核");
        }

        String auditComment = normalizeNullable(topicReviewRequest.getAuditComment());
        validateMaxLength(auditComment, MAX_AUDIT_COMMENT_LENGTH, "审核意见不能超过1000个字符");
        if (!topicReviewRequest.getApproved() && !StrUtil.isNotBlank(auditComment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "驳回课题时必须填写审核意见");
        }

        boolean updated = lambdaUpdate()
                .eq(Topic::getId, topic.getId())
                .set(Topic::getStatus, topicReviewRequest.getApproved()
                        ? TopicStatusEnum.APPROVED.name()
                        : TopicStatusEnum.REJECTED.name())
                .set(Topic::getReviewerid, admin.getId())
                .set(Topic::getAuditcomment, auditComment)
                .set(Topic::getReviewtime, new Date())
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "审核课题失败");
        }
        return true;
    }

    @Override
    public TopicVO getTopicVoById(long id) {
        Topic topic = getTopicOrThrow(id);
        checkViewPermission(topic, userService.getLoginUser());
        return toTopicVo(topic, userService.getById(topic.getTeacherid()));
    }

    @Override
    public IPage<TopicVO> listTopicVoByPage(TopicQueryRequest topicQueryRequest) {
        if (topicQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        if (topicQueryRequest.getCurrent() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (topicQueryRequest.getPageSize() <= 0 || topicQueryRequest.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1到50之间");
        }

        User currentUser = userService.getLoginUser();
        LambdaQueryWrapper<Topic> queryWrapper = new LambdaQueryWrapper<>();
        applyRoleScope(queryWrapper, currentUser, topicQueryRequest);
        queryWrapper.eq(topicQueryRequest.getId() != null, Topic::getId, topicQueryRequest.getId());
        queryWrapper.like(StrUtil.isNotBlank(topicQueryRequest.getTitle()),
                Topic::getTitle, normalizeNullable(topicQueryRequest.getTitle()));
        queryWrapper.eq(StrUtil.isNotBlank(topicQueryRequest.getType()),
                Topic::getType, normalizeNullable(topicQueryRequest.getType()));
        applyTopicSort(queryWrapper, topicQueryRequest.getSortField(), topicQueryRequest.getSortOrder());

        Page<Topic> topicPage = page(
                new Page<>(topicQueryRequest.getCurrent(), topicQueryRequest.getPageSize()),
                queryWrapper
        );
        Map<Long, User> teacherMap = getTeacherMap(topicPage.getRecords());
        Page<TopicVO> topicVoPage = new Page<>(
                topicPage.getCurrent(),
                topicPage.getSize(),
                topicPage.getTotal()
        );
        topicVoPage.setRecords(topicPage.getRecords().stream()
                .map(topic -> toTopicVo(topic, teacherMap.get(topic.getTeacherid())))
                .toList());
        return topicVoPage;
    }

    private void applyRoleScope(LambdaQueryWrapper<Topic> queryWrapper, User currentUser,
                                TopicQueryRequest request) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            if (StrUtil.isNotBlank(request.getStatus())) {
                validateTopicStatus(request.getStatus());
                queryWrapper.eq(Topic::getStatus, request.getStatus());
            }
            queryWrapper.eq(request.getTeacherId() != null,
                    Topic::getTeacherid, request.getTeacherId());
            return;
        }
        if (UserConstant.TEACHER_ROLE.equals(role)) {
            queryWrapper.eq(Topic::getTeacherid, currentUser.getId());
            if (StrUtil.isNotBlank(request.getStatus())) {
                validateTopicStatus(request.getStatus());
                queryWrapper.eq(Topic::getStatus, request.getStatus());
            }
            return;
        }
        if (UserConstant.STUDENT_ROLE.equals(role)) {
            queryWrapper.eq(Topic::getStatus, TopicStatusEnum.APPROVED.name());
            queryWrapper.eq(request.getTeacherId() != null,
                    Topic::getTeacherid, request.getTeacherId());
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查询课题");
    }

    private void checkViewPermission(Topic topic, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            return;
        }
        if (UserConstant.TEACHER_ROLE.equals(role)
                && currentUser.getId().equals(topic.getTeacherid())) {
            return;
        }
        if (UserConstant.STUDENT_ROLE.equals(role)
                && TopicStatusEnum.APPROVED.name().equals(topic.getStatus())) {
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该课题");
    }

    private User getCurrentTeacher() {
        User currentUser = userService.getLoginUser();
        if (!UserConstant.TEACHER_ROLE.equals(currentUser.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可以执行该操作");
        }
        return currentUser;
    }

    private User getCurrentAdmin() {
        User currentUser = userService.getLoginUser();
        if (!UserConstant.ADMIN_ROLE.equals(currentUser.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可以审核课题");
        }
        return currentUser;
    }

    private void checkTeacherOwner(Topic topic, User teacher) {
        if (!teacher.getId().equals(topic.getTeacherid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作其他教师的课题");
        }
    }

    private void checkEditable(Topic topic) {
        String status = topic.getStatus();
        if (!TopicStatusEnum.DRAFT.name().equals(status)
                && !TopicStatusEnum.REJECTED.name().equals(status)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前课题状态不允许编辑或提交");
        }
    }

    private Topic getTopicOrThrow(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课题主键不合法");
        }
        Topic topic = getById(id);
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课题不存在");
        }
        return topic;
    }

    private String validateAndNormalizeTitle(String title) {
        String normalizedTitle = normalize(title);
        if (StrUtil.isBlank(normalizedTitle)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课题名称不能为空");
        }
        validateMaxLength(normalizedTitle, MAX_TITLE_LENGTH, "课题名称不能超过255个字符");
        return normalizedTitle;
    }

    private String validateAndNormalizeDescription(String description) {
        String normalizedDescription = normalizeNullable(description);
        validateMaxLength(normalizedDescription, MAX_DESCRIPTION_LENGTH, "课题简介不能超过5000个字符");
        return normalizedDescription;
    }

    private String validateAndNormalizeType(String type) {
        String normalizedType = normalizeNullable(type);
        validateMaxLength(normalizedType, MAX_TYPE_LENGTH, "课题类型不能超过64个字符");
        return normalizedType;
    }

    private int validateMaxStudents(int maxStudents) {
        if (maxStudents < 1 || maxStudents > MAX_STUDENTS_LIMIT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "可选人数必须在1到20之间");
        }
        return maxStudents;
    }

    private void validateTopicStatus(String status) {
        if (TopicStatusEnum.getByValue(status) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课题状态不合法");
        }
    }

    private void validateMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, message);
        }
    }

    private Map<Long, User> getTeacherMap(List<Topic> topics) {
        if (topics.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> teacherIds = topics.stream()
                .map(Topic::getTeacherid)
                .collect(Collectors.toSet());
        return userService.listByIds(teacherIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private TopicVO toTopicVo(Topic topic, User teacher) {
        return TopicVO.fromTopic(topic, teacher);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeNullable(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private static void applyTopicSort(LambdaQueryWrapper<Topic> queryWrapper,
                                       String sortField, String sortOrder) {
        boolean ascending = "ascend".equalsIgnoreCase(sortOrder);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByDesc(Topic::getUpdatetime);
            return;
        }
        switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, Topic::getId);
            case "title" -> queryWrapper.orderBy(true, ascending, Topic::getTitle);
            case "type" -> queryWrapper.orderBy(true, ascending, Topic::getType);
            case "maxStudents" -> queryWrapper.orderBy(true, ascending, Topic::getMaxstudents);
            case "status" -> queryWrapper.orderBy(true, ascending, Topic::getStatus);
            case "createTime" -> queryWrapper.orderBy(true, ascending, Topic::getCreatetime);
            case "updateTime" -> queryWrapper.orderBy(true, ascending, Topic::getUpdatetime);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序字段不合法");
        }
    }

}
