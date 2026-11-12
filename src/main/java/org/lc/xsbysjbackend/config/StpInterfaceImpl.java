package org.lc.xsbysjbackend.config;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.mapper.UserMapper;
import org.lc.xsbysjbackend.model.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Sa-Token 角色数据源。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    private final PermissionConfig permissionConfig;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        User user = getUser(loginId);
        if (user == null) {
            return List.of();
        }
        return List.copyOf(permissionConfig.getPermissions(user.getRole()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = getUser(loginId);
        if (user == null || !StringUtils.hasText(user.getRole())) {
            return List.of();
        }
        return List.of(user.getRole());
    }

    private User getUser(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return userMapper.selectById(Long.parseLong(loginId.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
