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

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return List.of();
        }
        try {
            User user = userMapper.selectById(Long.parseLong(loginId.toString()));
            if (user == null || !StringUtils.hasText(user.getRole())) {
                return List.of();
            }
            return List.of(user.getRole());
        } catch (NumberFormatException e) {
            return List.of();
        }
    }
}
