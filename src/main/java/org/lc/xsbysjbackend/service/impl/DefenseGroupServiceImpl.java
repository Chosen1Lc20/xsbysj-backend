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
import org.lc.xsbysjbackend.mapper.DefenseGroupMapper;
import org.lc.xsbysjbackend.mapper.DefenseGroupStudentMapper;
import org.lc.xsbysjbackend.mapper.DefenseGroupTeacherMapper;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupAddRequest;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupQueryRequest;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupUpdateRequest;
import org.lc.xsbysjbackend.model.entity.DefenseGroup;
import org.lc.xsbysjbackend.model.entity.DefenseGroupStudent;
import org.lc.xsbysjbackend.model.entity.DefenseGroupTeacher;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.model.enums.SelectionStatusEnum;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.model.vo.DefenseGroupVO;
import org.lc.xsbysjbackend.model.vo.DefenseStudentVO;
import org.lc.xsbysjbackend.model.vo.DefenseTeacherVO;
import org.lc.xsbysjbackend.service.DefenseGroupService;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 答辩组服务实现。
 */
@Service
@RequiredArgsConstructor
public class DefenseGroupServiceImpl extends ServiceImpl<DefenseGroupMapper, DefenseGroup>
        implements DefenseGroupService {

    private static final int MAX_GROUP_NAME_LENGTH = 64;

    private static final int MAX_LOCATION_LENGTH = 255;

    private static final int MAX_REMARK_LENGTH = 500;

    private final DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    private final DefenseGroupStudentMapper defenseGroupStudentMapper;

    private final UserService userService;

    private final SelectionService selectionService;

    private final TopicService topicService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addDefenseGroup(DefenseGroupAddRequest defenseGroupAddRequest) {
        if (defenseGroupAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新增答辩组请求不能为空");
        }
        getCurrentAdmin();
        String groupName = validateGroupName(defenseGroupAddRequest.getGroupName());
        validateDefenseTime(defenseGroupAddRequest.getDefenseTime());
        String location = validateLocation(defenseGroupAddRequest.getLocation());
        String remark = validateRemark(defenseGroupAddRequest.getRemark());
        List<Long> teacherIds = resolveTeacherIds(
                defenseGroupAddRequest.getTeacherIds(),
                true
        );
        List<StudentAssignment> studentAssignments = resolveStudentAssignments(
                defenseGroupAddRequest.getStudentIds(),
                null
        );
        checkGroupNameUnique(groupName, null);

        DefenseGroup defenseGroup = new DefenseGroup();
        defenseGroup.setGroupname(groupName);
        defenseGroup.setDefensetime(defenseGroupAddRequest.getDefenseTime());
        defenseGroup.setLocation(location);
        defenseGroup.setRemark(remark);
        try {
            if (!save(defenseGroup)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增答辩组失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "答辩组名称已存在");
        }

        replaceTeachers(defenseGroup.getId(), teacherIds);
        replaceStudents(defenseGroup.getId(), studentAssignments);
        return defenseGroup.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDefenseGroup(DefenseGroupUpdateRequest defenseGroupUpdateRequest) {
        if (defenseGroupUpdateRequest == null || defenseGroupUpdateRequest.getId() == null
                || defenseGroupUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩组主键不能为空");
        }
        getCurrentAdmin();
        DefenseGroup defenseGroup = getDefenseGroupOrThrow(defenseGroupUpdateRequest.getId());

        LambdaUpdateWrapper<DefenseGroup> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DefenseGroup::getId, defenseGroup.getId());
        boolean hasUpdate = false;
        if (defenseGroupUpdateRequest.getGroupName() != null) {
            String groupName = validateGroupName(defenseGroupUpdateRequest.getGroupName());
            checkGroupNameUnique(groupName, defenseGroup.getId());
            updateWrapper.set(DefenseGroup::getGroupname, groupName);
            hasUpdate = true;
        }
        if (defenseGroupUpdateRequest.getDefenseTime() != null) {
            validateDefenseTime(defenseGroupUpdateRequest.getDefenseTime());
            updateWrapper.set(
                    DefenseGroup::getDefensetime,
                    defenseGroupUpdateRequest.getDefenseTime()
            );
            hasUpdate = true;
        }
        if (defenseGroupUpdateRequest.getLocation() != null) {
            updateWrapper.set(
                    DefenseGroup::getLocation,
                    validateLocation(defenseGroupUpdateRequest.getLocation())
            );
            hasUpdate = true;
        }
        if (defenseGroupUpdateRequest.getRemark() != null) {
            updateWrapper.set(
                    DefenseGroup::getRemark,
                    validateRemark(defenseGroupUpdateRequest.getRemark())
            );
            hasUpdate = true;
        }

        List<Long> teacherIds = null;
        if (defenseGroupUpdateRequest.getTeacherIds() != null) {
            teacherIds = resolveTeacherIds(defenseGroupUpdateRequest.getTeacherIds(), true);
            hasUpdate = true;
        }
        List<StudentAssignment> studentAssignments = null;
        if (defenseGroupUpdateRequest.getStudentIds() != null) {
            studentAssignments = resolveStudentAssignments(
                    defenseGroupUpdateRequest.getStudentIds(),
                    defenseGroup.getId()
            );
            hasUpdate = true;
        }
        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有需要更新的字段");
        }

        updateWrapper.set(DefenseGroup::getUpdatetime, new Date());
        if (!update(updateWrapper)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新答辩组失败");
        }
        if (teacherIds != null) {
            replaceTeachers(defenseGroup.getId(), teacherIds);
        }
        if (studentAssignments != null) {
            replaceStudents(defenseGroup.getId(), studentAssignments);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDefenseGroup(long id) {
        getCurrentAdmin();
        DefenseGroup defenseGroup = getDefenseGroupOrThrow(id);
        deleteTeacherLinks(defenseGroup.getId());
        deleteStudentLinks(defenseGroup.getId());
        if (!removeById(defenseGroup.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除答辩组失败");
        }
        return true;
    }

    @Override
    public DefenseGroupVO getDefenseGroupVoById(long id) {
        DefenseGroup defenseGroup = getDefenseGroupOrThrow(id);
        User currentUser = userService.getLoginUser();
        checkViewPermission(defenseGroup.getId(), currentUser);
        return toDefenseGroupVoList(List.of(defenseGroup)).get(0);
    }

    @Override
    public IPage<DefenseGroupVO> listDefenseGroupVoByPage(
            DefenseGroupQueryRequest defenseGroupQueryRequest) {
        validatePageRequest(defenseGroupQueryRequest);
        User currentUser = userService.getLoginUser();
        LambdaQueryWrapper<DefenseGroup> queryWrapper = new LambdaQueryWrapper<>();
        if (!applyRoleScope(queryWrapper, currentUser)) {
            return emptyPage(defenseGroupQueryRequest);
        }

        queryWrapper.eq(defenseGroupQueryRequest.getId() != null,
                DefenseGroup::getId, defenseGroupQueryRequest.getId());
        queryWrapper.like(StrUtil.isNotBlank(defenseGroupQueryRequest.getGroupName()),
                DefenseGroup::getGroupname,
                normalizeNullable(defenseGroupQueryRequest.getGroupName()));
        queryWrapper.like(StrUtil.isNotBlank(defenseGroupQueryRequest.getLocation()),
                DefenseGroup::getLocation,
                normalizeNullable(defenseGroupQueryRequest.getLocation()));
        queryWrapper.ge(defenseGroupQueryRequest.getDefenseTimeStart() != null,
                DefenseGroup::getDefensetime, defenseGroupQueryRequest.getDefenseTimeStart());
        queryWrapper.le(defenseGroupQueryRequest.getDefenseTimeEnd() != null,
                DefenseGroup::getDefensetime, defenseGroupQueryRequest.getDefenseTimeEnd());

        if (defenseGroupQueryRequest.getTeacherId() != null) {
            List<Long> groupIds = getGroupIdsByTeacher(defenseGroupQueryRequest.getTeacherId());
            if (groupIds.isEmpty()) {
                return emptyPage(defenseGroupQueryRequest);
            }
            queryWrapper.in(DefenseGroup::getId, groupIds);
        }
        if (defenseGroupQueryRequest.getStudentId() != null) {
            List<Long> groupIds = getGroupIdsByStudent(defenseGroupQueryRequest.getStudentId());
            if (groupIds.isEmpty()) {
                return emptyPage(defenseGroupQueryRequest);
            }
            queryWrapper.in(DefenseGroup::getId, groupIds);
        }
        if (defenseGroupQueryRequest.getSelectionId() != null) {
            List<Long> groupIds = getGroupIdsBySelection(
                    defenseGroupQueryRequest.getSelectionId()
            );
            if (groupIds.isEmpty()) {
                return emptyPage(defenseGroupQueryRequest);
            }
            queryWrapper.in(DefenseGroup::getId, groupIds);
        }
        applyDefenseGroupSort(
                queryWrapper,
                defenseGroupQueryRequest.getSortField(),
                defenseGroupQueryRequest.getSortOrder()
        );

        Page<DefenseGroup> groupPage = page(
                new Page<>(defenseGroupQueryRequest.getCurrent(),
                        defenseGroupQueryRequest.getPageSize()),
                queryWrapper
        );
        Page<DefenseGroupVO> voPage = new Page<>(
                groupPage.getCurrent(),
                groupPage.getSize(),
                groupPage.getTotal()
        );
        voPage.setRecords(toDefenseGroupVoList(groupPage.getRecords()));
        return voPage;
    }

    private boolean applyRoleScope(LambdaQueryWrapper<DefenseGroup> queryWrapper,
                                   User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            return true;
        }
        List<Long> groupIds;
        if (UserConstant.TEACHER_ROLE.equals(role)) {
            groupIds = getGroupIdsByTeacher(currentUser.getId());
        } else if (UserConstant.STUDENT_ROLE.equals(role)) {
            groupIds = getGroupIdsByStudent(currentUser.getId());
        } else {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查询答辩组");
        }
        if (groupIds.isEmpty()) {
            return false;
        }
        queryWrapper.in(DefenseGroup::getId, groupIds);
        return true;
    }

    private void checkViewPermission(long groupId, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            return;
        }
        if (UserConstant.TEACHER_ROLE.equals(role)
                && defenseGroupTeacherMapper.selectCount(
                new LambdaQueryWrapper<DefenseGroupTeacher>()
                        .eq(DefenseGroupTeacher::getGroupid, groupId)
                        .eq(DefenseGroupTeacher::getTeacherid, currentUser.getId())
        ) > 0) {
            return;
        }
        if (UserConstant.STUDENT_ROLE.equals(role)
                && defenseGroupStudentMapper.selectCount(
                new LambdaQueryWrapper<DefenseGroupStudent>()
                        .eq(DefenseGroupStudent::getGroupid, groupId)
                        .eq(DefenseGroupStudent::getStudentid, currentUser.getId())
        ) > 0) {
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该答辩组");
    }

    private List<Long> resolveTeacherIds(List<Long> teacherIds, boolean required) {
        List<Long> normalizedIds = normalizeIds(teacherIds, required, "答辩教师");
        if (normalizedIds.isEmpty()) {
            return normalizedIds;
        }
        Map<Long, User> userMap = userService.listByIds(normalizedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        for (Long teacherId : normalizedIds) {
            User teacher = userMap.get(teacherId);
            if (teacher == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "答辩教师不存在");
            }
            if (!UserConstant.TEACHER_ROLE.equals(teacher.getRole())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩组教师必须是教师账号");
            }
        }
        return normalizedIds;
    }

    private List<StudentAssignment> resolveStudentAssignments(List<Long> studentIds,
                                                               Long excludeGroupId) {
        List<Long> normalizedIds = normalizeIds(studentIds, false, "答辩学生");
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userService.listByIds(normalizedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        for (Long studentId : normalizedIds) {
            User student = userMap.get(studentId);
            if (student == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "答辩学生不存在");
            }
            if (!UserConstant.STUDENT_ROLE.equals(student.getRole())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩组学生必须是学生账号");
            }
        }

        List<DefenseGroupStudent> existingLinks = defenseGroupStudentMapper.selectList(
                new LambdaQueryWrapper<DefenseGroupStudent>()
                        .in(DefenseGroupStudent::getStudentid, normalizedIds)
        );
        boolean hasConflict = existingLinks.stream()
                .anyMatch(link -> excludeGroupId == null
                        || !excludeGroupId.equals(link.getGroupid()));
        if (hasConflict) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "学生已分配到其他答辩组");
        }

        List<Selection> selections = selectionService.lambdaQuery()
                .in(Selection::getStudentid, normalizedIds)
                .eq(Selection::getStatus, SelectionStatusEnum.APPROVED.name())
                .list();
        Map<Long, Selection> selectionMap = selections.stream()
                .collect(Collectors.toMap(
                        Selection::getStudentid,
                        Function.identity(),
                        (first, second) -> first
                ));
        if (selectionMap.size() != normalizedIds.size()) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR,
                    "存在尚未审核通过选题的学生，不能加入答辩组"
            );
        }

        Set<Long> topicIds = selectionMap.values().stream()
                .map(Selection::getTopicid)
                .collect(Collectors.toSet());
        Map<Long, Topic> topicMap = topicService.listByIds(topicIds).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
        for (Selection selection : selectionMap.values()) {
            Topic topic = topicMap.get(selection.getTopicid());
            if (topic == null || !TopicStatusEnum.APPROVED.name().equals(topic.getStatus())) {
                throw new BusinessException(
                        ErrorCode.OPERATION_ERROR,
                        "学生选题对应课题尚未审核通过"
                );
            }
        }

        return normalizedIds.stream()
                .map(studentId -> new StudentAssignment(
                        studentId,
                        selectionMap.get(studentId).getId()
                ))
                .toList();
    }

    private List<Long> normalizeIds(List<Long> ids, boolean required, String fieldName) {
        if (ids == null || ids.isEmpty()) {
            if (required) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + "不能为空");
            }
            return List.of();
        }
        List<Long> normalizedIds = new ArrayList<>(ids);
        if (normalizedIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + "主键不合法");
        }
        if (new LinkedHashSet<>(normalizedIds).size() != normalizedIds.size()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + "不能重复");
        }
        return normalizedIds;
    }

    private void replaceTeachers(long groupId, List<Long> teacherIds) {
        deleteTeacherLinks(groupId);
        for (Long teacherId : teacherIds) {
            DefenseGroupTeacher link = new DefenseGroupTeacher();
            link.setGroupid(groupId);
            link.setTeacherid(teacherId);
            defenseGroupTeacherMapper.insert(link);
        }
    }

    private void replaceStudents(long groupId, List<StudentAssignment> studentAssignments) {
        deleteStudentLinks(groupId);
        for (StudentAssignment assignment : studentAssignments) {
            DefenseGroupStudent link = new DefenseGroupStudent();
            link.setGroupid(groupId);
            link.setStudentid(assignment.studentId());
            link.setSelectionid(assignment.selectionId());
            defenseGroupStudentMapper.insert(link);
        }
    }

    private void deleteTeacherLinks(long groupId) {
        defenseGroupTeacherMapper.delete(
                new LambdaQueryWrapper<DefenseGroupTeacher>()
                        .eq(DefenseGroupTeacher::getGroupid, groupId)
        );
    }

    private void deleteStudentLinks(long groupId) {
        defenseGroupStudentMapper.delete(
                new LambdaQueryWrapper<DefenseGroupStudent>()
                        .eq(DefenseGroupStudent::getGroupid, groupId)
        );
    }

    private List<Long> getGroupIdsByTeacher(long teacherId) {
        return defenseGroupTeacherMapper.selectList(
                        new LambdaQueryWrapper<DefenseGroupTeacher>()
                                .eq(DefenseGroupTeacher::getTeacherid, teacherId)
                ).stream()
                .map(DefenseGroupTeacher::getGroupid)
                .distinct()
                .toList();
    }

    private List<Long> getGroupIdsByStudent(long studentId) {
        return defenseGroupStudentMapper.selectList(
                        new LambdaQueryWrapper<DefenseGroupStudent>()
                                .eq(DefenseGroupStudent::getStudentid, studentId)
                ).stream()
                .map(DefenseGroupStudent::getGroupid)
                .distinct()
                .toList();
    }

    private List<Long> getGroupIdsBySelection(long selectionId) {
        return defenseGroupStudentMapper.selectList(
                        new LambdaQueryWrapper<DefenseGroupStudent>()
                                .eq(DefenseGroupStudent::getSelectionid, selectionId)
                ).stream()
                .map(DefenseGroupStudent::getGroupid)
                .distinct()
                .toList();
    }

    private List<DefenseGroupVO> toDefenseGroupVoList(List<DefenseGroup> defenseGroups) {
        if (defenseGroups.isEmpty()) {
            return List.of();
        }
        Set<Long> groupIds = defenseGroups.stream()
                .map(DefenseGroup::getId)
                .collect(Collectors.toSet());
        List<DefenseGroupTeacher> teacherLinks = defenseGroupTeacherMapper.selectList(
                new LambdaQueryWrapper<DefenseGroupTeacher>()
                        .in(DefenseGroupTeacher::getGroupid, groupIds)
        );
        List<DefenseGroupStudent> studentLinks = defenseGroupStudentMapper.selectList(
                new LambdaQueryWrapper<DefenseGroupStudent>()
                        .in(DefenseGroupStudent::getGroupid, groupIds)
        );

        Set<Long> teacherIds = teacherLinks.stream()
                .map(DefenseGroupTeacher::getTeacherid)
                .collect(Collectors.toSet());
        Set<Long> studentIds = studentLinks.stream()
                .map(DefenseGroupStudent::getStudentid)
                .collect(Collectors.toSet());
        Set<Long> selectionIds = studentLinks.stream()
                .map(DefenseGroupStudent::getSelectionid)
                .collect(Collectors.toSet());

        Map<Long, Selection> selectionMap = selectionIds.isEmpty()
                ? Collections.emptyMap()
                : selectionService.listByIds(selectionIds).stream()
                .collect(Collectors.toMap(Selection::getId, Function.identity()));
        Set<Long> topicIds = selectionMap.values().stream()
                .map(Selection::getTopicid)
                .collect(Collectors.toSet());
        Map<Long, Topic> topicMap = topicIds.isEmpty()
                ? Collections.emptyMap()
                : topicService.listByIds(topicIds).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));

        Set<Long> userIds = new LinkedHashSet<>();
        userIds.addAll(teacherIds);
        userIds.addAll(studentIds);
        topicMap.values().stream()
                .map(Topic::getTeacherid)
                .filter(id -> id != null)
                .forEach(userIds::add);
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, List<DefenseGroupTeacher>> teacherLinksByGroup =
                teacherLinks.stream().collect(Collectors.groupingBy(
                        DefenseGroupTeacher::getGroupid
                ));
        Map<Long, List<DefenseGroupStudent>> studentLinksByGroup =
                studentLinks.stream().collect(Collectors.groupingBy(
                        DefenseGroupStudent::getGroupid
                ));

        return defenseGroups.stream()
                .map(group -> {
                    DefenseGroupVO vo = new DefenseGroupVO();
                    vo.setId(group.getId());
                    vo.setGroupName(group.getGroupname());
                    vo.setDefenseTime(group.getDefensetime());
                    vo.setLocation(group.getLocation());
                    vo.setRemark(group.getRemark());
                    vo.setTeachers(teacherLinksByGroup
                            .getOrDefault(group.getId(), List.of())
                            .stream()
                            .map(link -> toDefenseTeacherVo(link, userMap))
                            .toList());
                    vo.setStudents(studentLinksByGroup
                            .getOrDefault(group.getId(), List.of())
                            .stream()
                            .map(link -> toDefenseStudentVo(
                                    link,
                                    selectionMap,
                                    topicMap,
                                    userMap
                            ))
                            .toList());
                    vo.setCreateTime(group.getCreatetime());
                    vo.setEditTime(group.getEdittime());
                    vo.setUpdateTime(group.getUpdatetime());
                    return vo;
                })
                .toList();
    }

    private DefenseTeacherVO toDefenseTeacherVo(DefenseGroupTeacher link,
                                                Map<Long, User> userMap) {
        User teacher = userMap.get(link.getTeacherid());
        DefenseTeacherVO vo = new DefenseTeacherVO();
        vo.setId(link.getId());
        vo.setTeacherId(link.getTeacherid());
        if (teacher != null) {
            vo.setTeacherNo(teacher.getUserNo());
            vo.setTeacherName(teacher.getName());
            vo.setTeacherDepartment(teacher.getDepartment());
        }
        return vo;
    }

    private DefenseStudentVO toDefenseStudentVo(
            DefenseGroupStudent link,
            Map<Long, Selection> selectionMap,
            Map<Long, Topic> topicMap,
            Map<Long, User> userMap) {
        User student = userMap.get(link.getStudentid());
        Selection selection = selectionMap.get(link.getSelectionid());
        Topic topic = selection == null ? null : topicMap.get(selection.getTopicid());
        User advisor = topic == null ? null : userMap.get(topic.getTeacherid());

        DefenseStudentVO vo = new DefenseStudentVO();
        vo.setId(link.getId());
        vo.setStudentId(link.getStudentid());
        if (student != null) {
            vo.setStudentNo(student.getUserNo());
            vo.setStudentName(student.getName());
            vo.setStudentDepartment(student.getDepartment());
        }
        vo.setSelectionId(link.getSelectionid());
        if (topic != null) {
            vo.setTopicId(topic.getId());
            vo.setTopicTitle(topic.getTitle());
            vo.setTeacherId(topic.getTeacherid());
        }
        if (advisor != null) {
            vo.setTeacherName(advisor.getName());
        }
        return vo;
    }

    private void checkGroupNameUnique(String groupName, Long excludeId) {
        long count = lambdaQuery()
                .eq(DefenseGroup::getGroupname, groupName)
                .ne(excludeId != null, DefenseGroup::getId, excludeId)
                .count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "答辩组名称已存在");
        }
    }

    private String validateGroupName(String groupName) {
        String normalized = normalizeNullable(groupName);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩组名称不能为空");
        }
        if (normalized.length() > MAX_GROUP_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩组名称不能超过64个字符");
        }
        return normalized;
    }

    private String validateLocation(String location) {
        String normalized = normalizeNullable(location);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩地点不能为空");
        }
        if (normalized.length() > MAX_LOCATION_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩地点不能超过255个字符");
        }
        return normalized;
    }

    private String validateRemark(String remark) {
        String normalized = normalizeNullable(remark);
        if (normalized != null && normalized.length() > MAX_REMARK_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩备注不能超过500个字符");
        }
        return normalized;
    }

    private void validateDefenseTime(Date defenseTime) {
        if (defenseTime == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩时间不能为空");
        }
    }

    private DefenseGroup getDefenseGroupOrThrow(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩组主键不合法");
        }
        DefenseGroup defenseGroup = getById(id);
        if (defenseGroup == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "答辩组不存在");
        }
        return defenseGroup;
    }

    private User getCurrentAdmin() {
        User user = userService.getLoginUser();
        if (!UserConstant.ADMIN_ROLE.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可以维护答辩组");
        }
        return user;
    }

    private void validatePageRequest(DefenseGroupQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        if (request.getCurrent() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1到50之间");
        }
        if (request.getDefenseTimeStart() != null && request.getDefenseTimeEnd() != null
                && request.getDefenseTimeStart().after(request.getDefenseTimeEnd())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩时间范围不合法");
        }
    }

    private static String normalizeNullable(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private static IPage<DefenseGroupVO> emptyPage(DefenseGroupQueryRequest request) {
        Page<DefenseGroupVO> page = new Page<>(request.getCurrent(), request.getPageSize(), 0);
        page.setRecords(Collections.emptyList());
        return page;
    }

    private static void applyDefenseGroupSort(LambdaQueryWrapper<DefenseGroup> queryWrapper,
                                              String sortField, String sortOrder) {
        boolean ascending = "ascend".equalsIgnoreCase(sortOrder);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByAsc(DefenseGroup::getDefensetime);
            return;
        }
        switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, DefenseGroup::getId);
            case "groupName" -> queryWrapper.orderBy(
                    true, ascending, DefenseGroup::getGroupname
            );
            case "defenseTime" -> queryWrapper.orderBy(
                    true, ascending, DefenseGroup::getDefensetime
            );
            case "location" -> queryWrapper.orderBy(
                    true, ascending, DefenseGroup::getLocation
            );
            case "createTime" -> queryWrapper.orderBy(
                    true, ascending, DefenseGroup::getCreatetime
            );
            case "updateTime" -> queryWrapper.orderBy(
                    true, ascending, DefenseGroup::getUpdatetime
            );
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序字段不合法");
        }
    }

    private record StudentAssignment(long studentId, long selectionId) {
    }
}
