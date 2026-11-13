package org.lc.xsbysjbackend.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.common.BaseResponse;
import org.lc.xsbysjbackend.common.DeleteRequest;
import org.lc.xsbysjbackend.common.ResultUtils;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.constant.PermissionConstant;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public BaseResponse<String> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        userService.userRegister(userRegisterRequest);
        return ResultUtils.success("注册成功");
    }

    @PostMapping("/login")
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                          HttpServletRequest request) {
        User loginUser = userService.userLogin(userLoginRequest);

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(UserConstant.USER_LOGIN_STATE, loginUser.getId());

        StpUtil.login(loginUser.getId());
        return ResultUtils.success(UserVO.fromUser(loginUser));
    }

    @GetMapping("/me")
    public BaseResponse<UserVO> getLoginUser() {
        return ResultUtils.success(UserVO.fromUser(userService.getLoginUser()));
    }

    @PostMapping("/logout")
    public BaseResponse<String> userLogout(HttpServletRequest request) {
        StpUtil.logout();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResultUtils.success("退出成功");
    }

    @SaCheckPermission(PermissionConstant.USER_ADD)
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        return ResultUtils.success(userService.addUser(userAddRequest));
    }

    @SaCheckPermission(PermissionConstant.USER_DELETE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUserById(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户主键不能为空");
        }
        return ResultUtils.success(userService.deleteUserById(deleteRequest.getId()));
    }

    @SaCheckPermission(PermissionConstant.USER_GET)
    @GetMapping("/get")
    public BaseResponse<User> getUserById(@RequestParam long id) {
        return ResultUtils.success(userService.getUserById(id));
    }

    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVoById(@RequestParam long id) {
        return ResultUtils.success(userService.getUserVoById(id));
    }

    @SaCheckPermission(PermissionConstant.USER_TEACHER_LIST)
    @GetMapping("/teachers")
    public BaseResponse<List<TeacherOptionVO>> listTeacherOptions() {
        return ResultUtils.success(userService.listTeacherOptions());
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户主键不能为空");
        }
        return ResultUtils.success(userService.updateUser(userUpdateRequest));
    }

    @SaCheckPermission(PermissionConstant.USER_LIST)
    @PostMapping("/list/page/vo")
    public BaseResponse<IPage<UserVO>> listUserVoByPage(
            @RequestBody UserQueryRequest userQueryRequest) {
        return ResultUtils.success(userService.listUserVoByPage(userQueryRequest));
    }
}
