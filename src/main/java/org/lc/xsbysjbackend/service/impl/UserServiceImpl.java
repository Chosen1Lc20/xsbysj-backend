package org.lc.xsbysjbackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lc.xsbysjbackend.common.PasswordUtils;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.exception.BusinessException;
import org.lc.xsbysjbackend.exception.ErrorCode;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.model.dto.user.UserLoginRequest;
import org.lc.xsbysjbackend.model.dto.user.UserQueryRequest;
import org.lc.xsbysjbackend.model.dto.user.UserRegisterRequest;
import org.lc.xsbysjbackend.model.dto.user.UserUpdateRequest;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.model.vo.TeacherOptionVO;
import org.lc.xsbysjbackend.model.vo.UserVO;
import org.lc.xsbysjbackend.service.UserService;
import org.lc.xsbysjbackend.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author lianchao0921
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册请求不能为空");
        }

        String useraccount = normalize(userRegisterRequest.getUseraccount());
        String password = userRegisterRequest.getPassword();
        String confirmPassword = userRegisterRequest.getConfirmPassword();
        String userNo = normalize(userRegisterRequest.getUserNo());
        String name = normalize(userRegisterRequest.getName());
        String role = normalize(userRegisterRequest.getRole());
        String department = normalizeNullable(userRegisterRequest.getDepartment());

        if (StrUtil.isBlank(useraccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不能为空");
        }
        if (useraccount.length() < 4 || useraccount.length() > 23) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度必须在4到23之间");
        }
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码不能为空");
        }
        if (password.length() < 4 || password.length() > 13) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度必须在4到13之间");
        }
        if (StrUtil.isBlank(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "二次输入密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        if (StrUtil.isBlank(userNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号不能为空");
        }
        if (userNo.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号不能超过64个字符");
        }
        if (StrUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "姓名不能为空");
        }
        if (name.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "姓名不能超过64个字符");
        }
        if (department != null && department.length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "院系不能超过128个字符");
        }
        if (StrUtil.isBlank(role)) {
            role = UserConstant.STUDENT_ROLE;
        }
        if (!UserConstant.STUDENT_ROLE.equals(role)
                && !UserConstant.TEACHER_ROLE.equals(role)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "公开注册用户类型只能为学生或教师");
        }

        if (lambdaQuery().eq(User::getUseraccount, useraccount).count() > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号已存在");
        }
        if (lambdaQuery().eq(User::getUserNo, userNo).count() > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号已存在");
        }

        User user = new User();
        user.setUseraccount(useraccount);
        user.setUserNo(userNo);
        user.setPassword(PasswordUtils.encode(password));
        user.setName(name);
        user.setRole(role);
        user.setDepartment(department);
        try {
            if (!save(user)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或学号/工号已存在");
        }
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User userLogin(UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录请求不能为空");
        }

        String useraccount = normalize(userLoginRequest.getUseraccount());
        String password = userLoginRequest.getPassword();
        if (StrUtil.isBlank(useraccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不能为空");
        }
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码不能为空");
        }

        User user = lambdaQuery().eq(User::getUseraccount, useraccount).one();
        if (user == null || !PasswordUtils.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        if (!PasswordUtils.isEncoded(user.getPassword())) {
            String encodedPassword = PasswordUtils.encode(password);
            boolean updated = lambdaUpdate()
                    .eq(User::getId, user.getId())
                    .set(User::getPassword, encodedPassword)
                    .update();
            if (!updated) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "密码安全升级失败");
            }
            user.setPassword(encodedPassword);
        }
        return user;
    }

    @Override
    public User getLoginUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User user = getById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录用户不存在");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addUser(UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新增用户请求不能为空");
        }

        String useraccount = normalize(userAddRequest.getUseraccount());
        String password = userAddRequest.getPassword();
        String userNo = normalize(userAddRequest.getUserNo());
        String name = normalize(userAddRequest.getName());
        String role = normalize(userAddRequest.getRole());

        if (StrUtil.isBlank(useraccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不能为空");
        }
        if (useraccount.length() < 4 || useraccount.length() > 23) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度必须在4到23之间");
        }
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码不能为空");
        }
        if (password.length() < 4 || password.length() > 13) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度必须在4到13之间");
        }
        if (StrUtil.isBlank(userNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号不能为空");
        }
        if (userNo.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号不能超过64个字符");
        }
        if (StrUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "姓名不能为空");
        }
        if (name.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "姓名不能超过64个字符");
        }
        if (StrUtil.isBlank(role)) {
            role = UserConstant.STUDENT_ROLE;
        }
        if (!UserConstant.ADMIN_ROLE.equals(role)
                && !UserConstant.TEACHER_ROLE.equals(role)
                && !UserConstant.STUDENT_ROLE.equals(role)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色不合法");
        }
        if (lambdaQuery().eq(User::getUseraccount, useraccount).count() > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号已存在");
        }
        if (lambdaQuery().eq(User::getUserNo, userNo).count() > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号已存在");
        }

        User user = new User();
        user.setUseraccount(useraccount);
        user.setUserNo(userNo);
        user.setPassword(PasswordUtils.encode(password));
        user.setName(name);
        user.setRole(role);
        user.setDepartment(normalizeNullable(userAddRequest.getDepartment()));
        user.setAvatarUrl(normalizeNullable(userAddRequest.getAvatarUrl()));
        try {
            if (!save(user)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增用户失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或学号/工号已存在");
        }
        return user.getId();
    }

    @Override
    public boolean deleteUserById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户主键不合法");
        }
        if (getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (!removeById(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除用户失败");
        }
        StpUtil.logout(id);
        return true;
    }

    @Override
    public User getUserById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户主键不合法");
        }
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user;
    }

    @Override
    public UserVO getUserVoById(long id) {
        User currentUser = getLoginUser();
        if (!isAdmin(currentUser) && !currentUser.getId().equals(id)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看其他用户信息");
        }
        return UserVO.fromUser(getUserById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null
                || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新用户请求不合法");
        }

        Long userId = userUpdateRequest.getId();
        User oldUser = getById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        User currentUser = getLoginUser();
        if (!isAdmin(currentUser) && !currentUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权修改其他用户信息");
        }
        if (!isAdmin(currentUser) && userUpdateRequest.getRole() != null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "普通用户不能修改角色");
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        boolean hasUpdate = false;

        if (userUpdateRequest.getUseraccount() != null) {
            String useraccount = normalize(userUpdateRequest.getUseraccount());
            if (StrUtil.isBlank(useraccount)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不能为空");
            }
            if (useraccount.length() < 4 || useraccount.length() > 23) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度必须在4到23之间");
            }
            if (lambdaQuery().eq(User::getUseraccount, useraccount)
                    .ne(User::getId, userId).count() > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号已存在");
            }
            updateWrapper.set(User::getUseraccount, useraccount);
            hasUpdate = true;
        }
        if (userUpdateRequest.getPassword() != null) {
            String password = userUpdateRequest.getPassword();
            if (StrUtil.isBlank(password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码不能为空");
            }
            if (password.length() < 4 || password.length() > 13) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度必须在4到13之间");
            }
            updateWrapper.set(User::getPassword, PasswordUtils.encode(password));
            hasUpdate = true;
        }
        if (userUpdateRequest.getUserNo() != null) {
            String userNo = normalize(userUpdateRequest.getUserNo());
            if (StrUtil.isBlank(userNo)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号不能为空");
            }
            if (userNo.length() > 64) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号不能超过64个字符");
            }
            if (lambdaQuery().eq(User::getUserNo, userNo)
                    .ne(User::getId, userId).count() > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或工号已存在");
            }
            updateWrapper.set(User::getUserNo, userNo);
            hasUpdate = true;
        }
        if (userUpdateRequest.getName() != null) {
            String name = normalize(userUpdateRequest.getName());
            if (StrUtil.isBlank(name)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "姓名不能为空");
            }
            if (name.length() > 64) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "姓名不能超过64个字符");
            }
            updateWrapper.set(User::getName, name);
            hasUpdate = true;
        }
        if (userUpdateRequest.getRole() != null) {
            String role = normalize(userUpdateRequest.getRole());
            if (!UserConstant.ADMIN_ROLE.equals(role)
                    && !UserConstant.TEACHER_ROLE.equals(role)
                    && !UserConstant.STUDENT_ROLE.equals(role)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色不合法");
            }
            updateWrapper.set(User::getRole, role);
            hasUpdate = true;
        }
        if (userUpdateRequest.getDepartment() != null) {
            String department = normalizeNullable(userUpdateRequest.getDepartment());
            if (department != null && department.length() > 128) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "院系不能超过128个字符");
            }
            updateWrapper.set(User::getDepartment, department);
            hasUpdate = true;
        }
        if (userUpdateRequest.getAvatarUrl() != null) {
            updateWrapper.set(User::getAvatarUrl, normalizeNullable(userUpdateRequest.getAvatarUrl()));
            hasUpdate = true;
        }
        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有需要更新的字段");
        }
        try {
            if (!update(updateWrapper)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新用户失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或学号/工号已存在");
        }
        return true;
    }

    @Override
    public IPage<UserVO> listUserVoByPage(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        if (userQueryRequest.getCurrent() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (userQueryRequest.getPageSize() <= 0 || userQueryRequest.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1到50之间");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(userQueryRequest.getId() != null, User::getId, userQueryRequest.getId());
        queryWrapper.like(StrUtil.isNotBlank(userQueryRequest.getUseraccount()),
                User::getUseraccount, normalizeNullable(userQueryRequest.getUseraccount()));
        queryWrapper.like(StrUtil.isNotBlank(userQueryRequest.getUserNo()),
                User::getUserNo, normalizeNullable(userQueryRequest.getUserNo()));
        queryWrapper.like(StrUtil.isNotBlank(userQueryRequest.getName()),
                User::getName, normalizeNullable(userQueryRequest.getName()));
        queryWrapper.eq(StrUtil.isNotBlank(userQueryRequest.getRole()),
                User::getRole, normalizeNullable(userQueryRequest.getRole()));
        queryWrapper.like(StrUtil.isNotBlank(userQueryRequest.getDepartment()),
                User::getDepartment, normalizeNullable(userQueryRequest.getDepartment()));
        applyUserSort(queryWrapper, userQueryRequest.getSortField(), userQueryRequest.getSortOrder());

        Page<User> userPage = page(new Page<>(userQueryRequest.getCurrent(),
                userQueryRequest.getPageSize()), queryWrapper);
        Page<UserVO> userVoPage = new Page<>(userPage.getCurrent(), userPage.getSize(),
                userPage.getTotal());
        userVoPage.setRecords(userPage.getRecords().stream().map(UserVO::fromUser).toList());
        return userVoPage;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserConstant.ADMIN_ROLE.equals(user.getRole());
    }

    @Override
    public List<TeacherOptionVO> listTeacherOptions() {
        return lambdaQuery()
                .eq(User::getRole, UserConstant.TEACHER_ROLE)
                .select(
                        User::getId,
                        User::getUserNo,
                        User::getName,
                        User::getDepartment
                )
                .orderByAsc(User::getName)
                .list()
                .stream()
                .map(TeacherOptionVO::fromUser)
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeNullable(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private static void applyUserSort(LambdaQueryWrapper<User> queryWrapper,
                                      String sortField, String sortOrder) {
        boolean ascending = "ascend".equalsIgnoreCase(sortOrder);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByDesc(User::getUpdatetime);
            return;
        }
        switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, User::getId);
            case "useraccount" -> queryWrapper.orderBy(true, ascending, User::getUseraccount);
            case "userNo" -> queryWrapper.orderBy(true, ascending, User::getUserNo);
            case "name" -> queryWrapper.orderBy(true, ascending, User::getName);
            case "role" -> queryWrapper.orderBy(true, ascending, User::getRole);
            case "department" -> queryWrapper.orderBy(true, ascending, User::getDepartment);
            case "createTime" -> queryWrapper.orderBy(true, ascending, User::getCreatetime);
            case "UpdateTime" -> queryWrapper.orderBy(true, ascending, User::getUpdatetime);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序字段不合法");
        }
    }
}
