package org.lc.xsbysjbackend.service;

import org.lc.xsbysjbackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.model.dto.user.UserLoginRequest;
import org.lc.xsbysjbackend.model.dto.user.UserQueryRequest;
import org.lc.xsbysjbackend.model.dto.user.UserRegisterRequest;
import org.lc.xsbysjbackend.model.dto.user.UserUpdateRequest;
import org.lc.xsbysjbackend.model.vo.UserVO;

/**
* @author lianchao0921
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-11-11 15:19:31
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册。
     *
     * @param userRegisterRequest 注册信息
     * @return 新用户主键
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 校验账号和密码。
     *
     * @param userLoginRequest 登录信息
     * @return 登录用户
     */
    User userLogin(UserLoginRequest userLoginRequest);

    /**
     * 获取当前登录用户。
     *
     * @return 当前用户
     */
    User getLoginUser();

    /**
     * 管理员新增用户。
     */
    long addUser(UserAddRequest userAddRequest);

    /**
     * 根据主键删除用户。
     */
    boolean deleteUserById(long id);

    /**
     * 根据主键获取用户原始信息。
     */
    User getUserById(long id);

    /**
     * 根据主键获取脱敏用户信息。
     */
    UserVO getUserVoById(long id);

    /**
     * 更新用户信息。
     */
    boolean updateUser(UserUpdateRequest userUpdateRequest);

    /**
     * 分页获取脱敏用户列表。
     */
    IPage<UserVO> listUserVoByPage(UserQueryRequest userQueryRequest);

    /**
     * 判断用户是否为管理员。
     */
    boolean isAdmin(User user);
}
