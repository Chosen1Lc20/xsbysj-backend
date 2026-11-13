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
import org.lc.xsbysjbackend.mapper.AssignmentMapper;
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentAddRequest;
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentQueryRequest;
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentUpdateRequest;
import org.lc.xsbysjbackend.model.entity.Assignment;
import org.lc.xsbysjbackend.model.entity.FileRecord;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.model.enums.AssignmentStatusEnum;
import org.lc.xsbysjbackend.model.enums.FileTypeEnum;
import org.lc.xsbysjbackend.model.enums.SelectionStatusEnum;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.model.vo.AssignmentVO;
import org.lc.xsbysjbackend.service.AssignmentService;
import org.lc.xsbysjbackend.service.FileRecordService;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
* @description 针对表【assignment(任务书表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl extends ServiceImpl<AssignmentMapper, Assignment>
    implements AssignmentService {

    private static final int MAX_TITLE_LENGTH = 255;

    private final SelectionService selectionService;

    private final TopicService topicService;

    private final UserService userService;

    private final FileRecordService fileRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addAssignment(AssignmentAddRequest assignmentAddRequest) {
        if (assignmentAddRequest == null || assignmentAddRequest.getSelectionId() == null
                || assignmentAddRequest.getSelectionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选题主键不能为空");
        }
        User teacher = getCurrentTeacher();
        String title = validateTitle(assignmentAddRequest.getTitle());
        String content = normalizeNullable(assignmentAddRequest.getContent());

        Selection selection = getSelectionOrThrow(assignmentAddRequest.getSelectionId());
        if (!SelectionStatusEnum.APPROVED.name().equals(selection.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有审核通过的选题可以创建任务书");
        }
        Topic topic = getTopicOrThrow(selection.getTopicid());
        if (!TopicStatusEnum.APPROVED.name().equals(topic.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "选题对应课题尚未审核通过");
        }
        if (!teacher.getId().equals(topic.getTeacherid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权为其他教师课题下的选题创建任务书");
        }
        if (lambdaQuery().eq(Assignment::getSelectionid, selection.getId()).count() > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该选题已创建任务书");
        }
        validateAttachment(assignmentAddRequest.getFileRecordId(), teacher.getId());

        Assignment assignment = new Assignment();
        assignment.setSelectionid(selection.getId());
        assignment.setTeacherid(teacher.getId());
        assignment.setTopicid(topic.getId());
        assignment.setTitle(title);
        assignment.setContent(content);
        assignment.setFilerecordid(assignmentAddRequest.getFileRecordId());
        assignment.setDeadline(assignmentAddRequest.getDeadline());
        assignment.setStatus(AssignmentStatusEnum.DRAFT.name());
        try {
            if (!save(assignment)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建任务书失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该选题已创建任务书");
        }
        return assignment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAssignment(AssignmentUpdateRequest assignmentUpdateRequest) {
        if (assignmentUpdateRequest == null || assignmentUpdateRequest.getId() == null
                || assignmentUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务书主键不能为空");
        }
        User teacher = getCurrentTeacher();
        Assignment assignment = getAssignmentOrThrow(assignmentUpdateRequest.getId());
        checkTeacherOwner(assignment, teacher);
        checkDraft(assignment);

        LambdaUpdateWrapper<Assignment> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Assignment::getId, assignment.getId());
        updateWrapper.eq(Assignment::getTeacherid, teacher.getId());
        updateWrapper.eq(Assignment::getStatus, AssignmentStatusEnum.DRAFT.name());

        boolean hasUpdate = false;
        if (assignmentUpdateRequest.getTitle() != null) {
            updateWrapper.set(Assignment::getTitle,
                    validateTitle(assignmentUpdateRequest.getTitle()));
            hasUpdate = true;
        }
        if (assignmentUpdateRequest.getContent() != null) {
            updateWrapper.set(Assignment::getContent,
                    normalizeNullable(assignmentUpdateRequest.getContent()));
            hasUpdate = true;
        }
        if (assignmentUpdateRequest.getDeadline() != null) {
            updateWrapper.set(Assignment::getDeadline, assignmentUpdateRequest.getDeadline());
            hasUpdate = true;
        }
        if (assignmentUpdateRequest.getFileRecordId() != null) {
            validateAttachment(assignmentUpdateRequest.getFileRecordId(), teacher.getId());
            updateWrapper.set(Assignment::getFilerecordid,
                    assignmentUpdateRequest.getFileRecordId());
            hasUpdate = true;
        }
        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有需要更新的字段");
        }
        if (!update(updateWrapper)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新任务书失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAssignment(long id) {
        User teacher = getCurrentTeacher();
        Assignment assignment = getAssignmentOrThrow(id);
        checkTeacherOwner(assignment, teacher);
        checkDraft(assignment);

        boolean removed = lambdaUpdate()
                .eq(Assignment::getId, assignment.getId())
                .eq(Assignment::getTeacherid, teacher.getId())
                .eq(Assignment::getStatus, AssignmentStatusEnum.DRAFT.name())
                .remove();
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除任务书失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean issueAssignment(long id) {
        User teacher = getCurrentTeacher();
        Assignment assignment = getAssignmentOrThrow(id);
        checkTeacherOwner(assignment, teacher);
        checkDraft(assignment);

        boolean updated = lambdaUpdate()
                .eq(Assignment::getId, assignment.getId())
                .eq(Assignment::getTeacherid, teacher.getId())
                .eq(Assignment::getStatus, AssignmentStatusEnum.DRAFT.name())
                .set(Assignment::getStatus, AssignmentStatusEnum.ISSUED.name())
                .set(Assignment::getIssuetime, new Date())
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下发任务书失败");
        }
        return true;
    }

    @Override
    public AssignmentVO getAssignmentVoById(long id) {
        Assignment assignment = getAssignmentOrThrow(id);
        Selection selection = getSelectionOrThrow(assignment.getSelectionid());
        Topic topic = getTopicOrThrow(assignment.getTopicid());
        User currentUser = userService.getLoginUser();
        checkViewPermission(assignment, selection, currentUser);
        return AssignmentVO.fromAssignment(
                assignment,
                selection,
                topic,
                userService.getById(selection.getStudentid()),
                userService.getById(assignment.getTeacherid()),
                getFileRecord(assignment.getFilerecordid())
        );
    }

    @Override
    public IPage<AssignmentVO> listAssignmentVoByPage(
            AssignmentQueryRequest assignmentQueryRequest) {
        validatePageRequest(assignmentQueryRequest);
        User currentUser = userService.getLoginUser();
        LambdaQueryWrapper<Assignment> queryWrapper = new LambdaQueryWrapper<>();
        if (!applyRoleScope(queryWrapper, currentUser, assignmentQueryRequest)) {
            return emptyPage(assignmentQueryRequest);
        }

        queryWrapper.eq(assignmentQueryRequest.getId() != null,
                Assignment::getId, assignmentQueryRequest.getId());
        queryWrapper.eq(assignmentQueryRequest.getSelectionId() != null,
                Assignment::getSelectionid, assignmentQueryRequest.getSelectionId());
        queryWrapper.eq(assignmentQueryRequest.getTopicId() != null,
                Assignment::getTopicid, assignmentQueryRequest.getTopicId());
        queryWrapper.eq(assignmentQueryRequest.getTeacherId() != null,
                Assignment::getTeacherid, assignmentQueryRequest.getTeacherId());
        queryWrapper.like(StrUtil.isNotBlank(assignmentQueryRequest.getTitle()),
                Assignment::getTitle, normalizeNullable(assignmentQueryRequest.getTitle()));
        queryWrapper.eq(StrUtil.isNotBlank(assignmentQueryRequest.getStatus()),
                Assignment::getStatus, assignmentQueryRequest.getStatus());

        if (!UserConstant.STUDENT_ROLE.equals(currentUser.getRole())
                && assignmentQueryRequest.getStudentId() != null) {
            List<Long> studentSelectionIds = getStudentSelectionIds(
                    assignmentQueryRequest.getStudentId()
            );
            if (studentSelectionIds.isEmpty()) {
                return emptyPage(assignmentQueryRequest);
            }
            queryWrapper.in(Assignment::getSelectionid, studentSelectionIds);
        }
        applyAssignmentSort(
                queryWrapper,
                assignmentQueryRequest.getSortField(),
                assignmentQueryRequest.getSortOrder()
        );

        Page<Assignment> assignmentPage = page(
                new Page<>(assignmentQueryRequest.getCurrent(),
                        assignmentQueryRequest.getPageSize()),
                queryWrapper
        );
        Page<AssignmentVO> voPage = new Page<>(
                assignmentPage.getCurrent(),
                assignmentPage.getSize(),
                assignmentPage.getTotal()
        );
        voPage.setRecords(toAssignmentVoList(assignmentPage.getRecords()));
        return voPage;
    }

    private boolean applyRoleScope(LambdaQueryWrapper<Assignment> queryWrapper, User currentUser,
                                   AssignmentQueryRequest request) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            return true;
        }
        if (UserConstant.TEACHER_ROLE.equals(role)) {
            queryWrapper.eq(Assignment::getTeacherid, currentUser.getId());
            return true;
        }
        if (UserConstant.STUDENT_ROLE.equals(role)) {
            List<Long> selectionIds = getStudentSelectionIds(currentUser.getId());
            if (selectionIds.isEmpty()) {
                return false;
            }
            queryWrapper.in(Assignment::getSelectionid, selectionIds);
            queryWrapper.eq(Assignment::getStatus, AssignmentStatusEnum.ISSUED.name());
            return true;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查询任务书");
    }

    private void checkViewPermission(Assignment assignment, Selection selection, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)
                || (UserConstant.TEACHER_ROLE.equals(role)
                && currentUser.getId().equals(assignment.getTeacherid()))) {
            return;
        }
        if (UserConstant.STUDENT_ROLE.equals(role)
                && currentUser.getId().equals(selection.getStudentid())) {
            if (AssignmentStatusEnum.ISSUED.name().equals(assignment.getStatus())) {
                return;
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务书尚未下发");
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该任务书");
    }

    private void checkTeacherOwner(Assignment assignment, User teacher) {
        if (!teacher.getId().equals(assignment.getTeacherid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作其他教师的课题任务书");
        }
    }

    private void checkDraft(Assignment assignment) {
        if (!AssignmentStatusEnum.DRAFT.name().equals(assignment.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有草稿任务书可以执行该操作");
        }
    }

    private void validateAttachment(Long fileRecordId, Long teacherId) {
        if (fileRecordId == null) {
            return;
        }
        if (fileRecordId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "附件文件主键不合法");
        }
        FileRecord fileRecord = fileRecordService.getById(fileRecordId);
        if (fileRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "附件文件不存在");
        }
        if (!teacherId.equals(fileRecord.getUploadUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能使用本人上传的附件");
        }
        if (!FileTypeEnum.ATTACHMENT.getValue().equals(fileRecord.getFileType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务书附件必须是文档文件");
        }
    }

    private List<AssignmentVO> toAssignmentVoList(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        Set<Long> selectionIds = assignments.stream()
                .map(Assignment::getSelectionid)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> topicIds = assignments.stream()
                .map(Assignment::getTopicid)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> fileRecordIds = assignments.stream()
                .map(Assignment::getFilerecordid)
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
        Map<Long, FileRecord> fileRecordMap = fileRecordIds.isEmpty()
                ? Collections.emptyMap()
                : fileRecordService.listByIds(fileRecordIds).stream()
                .collect(Collectors.toMap(FileRecord::getId, Function.identity()));

        Set<Long> userIds = new LinkedHashSet<>();
        assignments.stream()
                .map(Assignment::getTeacherid)
                .filter(id -> id != null)
                .forEach(userIds::add);
        selectionMap.values().stream()
                .map(Selection::getStudentid)
                .filter(id -> id != null)
                .forEach(userIds::add);
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return assignments.stream()
                .map(assignment -> {
                    Selection selection = selectionMap.get(assignment.getSelectionid());
                    User student = selection == null ? null : userMap.get(selection.getStudentid());
                    return AssignmentVO.fromAssignment(
                            assignment,
                            selection,
                            topicMap.get(assignment.getTopicid()),
                            student,
                            userMap.get(assignment.getTeacherid()),
                            fileRecordMap.get(assignment.getFilerecordid())
                    );
                })
                .toList();
    }

    private FileRecord getFileRecord(Long fileRecordId) {
        return fileRecordId == null ? null : fileRecordService.getById(fileRecordId);
    }

    private List<Long> getStudentSelectionIds(Long studentId) {
        return selectionService.lambdaQuery()
                .eq(Selection::getStudentid, studentId)
                .select(Selection::getId)
                .list()
                .stream()
                .map(Selection::getId)
                .toList();
    }

    private Assignment getAssignmentOrThrow(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务书主键不合法");
        }
        Assignment assignment = getById(id);
        if (assignment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务书不存在");
        }
        return assignment;
    }

    private Selection getSelectionOrThrow(long id) {
        Selection selection = selectionService.getById(id);
        if (selection == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "选题记录不存在");
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

    private User getCurrentTeacher() {
        User user = userService.getLoginUser();
        if (!UserConstant.TEACHER_ROLE.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可以执行该操作");
        }
        return user;
    }

    private String validateTitle(String title) {
        String normalizedTitle = normalizeNullable(title);
        if (StrUtil.isBlank(normalizedTitle)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务书标题不能为空");
        }
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务书标题不能超过255个字符");
        }
        return normalizedTitle;
    }

    private void validatePageRequest(AssignmentQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        if (request.getCurrent() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1到50之间");
        }
        if (StrUtil.isNotBlank(request.getStatus())
                && AssignmentStatusEnum.getByValue(request.getStatus()) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务书状态不合法");
        }
    }

    private static String normalizeNullable(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private static IPage<AssignmentVO> emptyPage(AssignmentQueryRequest request) {
        Page<AssignmentVO> page = new Page<>(request.getCurrent(), request.getPageSize(), 0);
        page.setRecords(Collections.emptyList());
        return page;
    }

    private static void applyAssignmentSort(LambdaQueryWrapper<Assignment> queryWrapper,
                                            String sortField, String sortOrder) {
        boolean ascending = "ascend".equalsIgnoreCase(sortOrder);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByDesc(Assignment::getUpdatetime);
            return;
        }
        switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, Assignment::getId);
            case "title" -> queryWrapper.orderBy(true, ascending, Assignment::getTitle);
            case "deadline" -> queryWrapper.orderBy(true, ascending, Assignment::getDeadline);
            case "status" -> queryWrapper.orderBy(true, ascending, Assignment::getStatus);
            case "issueTime" -> queryWrapper.orderBy(true, ascending, Assignment::getIssuetime);
            case "createTime" -> queryWrapper.orderBy(
                    true, ascending, Assignment::getCreatetime
            );
            case "updateTime" -> queryWrapper.orderBy(
                    true, ascending, Assignment::getUpdatetime
            );
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序字段不合法");
        }
    }
}
