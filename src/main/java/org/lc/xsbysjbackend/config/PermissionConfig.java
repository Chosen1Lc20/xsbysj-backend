package org.lc.xsbysjbackend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lc.xsbysjbackend.constant.PermissionConstant;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 静态角色权限配置。
 */
@Component
public class PermissionConfig {

    private static final String CONFIG_PATH = "biz/permission.json";

    private final Map<String, Set<String>> rolePermissions;

    public PermissionConfig(ObjectMapper objectMapper) {
        this.rolePermissions = loadConfig(objectMapper);
    }

    public Set<String> getPermissions(String role) {
        if (!StringUtils.hasText(role)) {
            return Set.of();
        }
        return rolePermissions.getOrDefault(role, Set.of());
    }

    private Map<String, Set<String>> loadConfig(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(CONFIG_PATH);
        Map<String, List<String>> rawConfig;
        try (InputStream inputStream = resource.getInputStream()) {
            rawConfig = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, List<String>>>() {
                    }
            );
        } catch (IOException e) {
            throw new IllegalStateException("加载权限配置失败：" + CONFIG_PATH, e);
        }

        validateRequiredRoles(rawConfig.keySet());
        Map<String, Set<String>> loadedConfig = new LinkedHashMap<>();
        rawConfig.forEach((role, permissions) -> {
            if (!StringUtils.hasText(role) || permissions == null) {
                throw new IllegalStateException("权限配置中的角色和权限列表不能为空");
            }
            Set<String> permissionSet = new LinkedHashSet<>();
            for (String permission : permissions) {
                if (!StringUtils.hasText(permission)) {
                    throw new IllegalStateException("角色 " + role + " 包含空权限码");
                }
                if (!PermissionConstant.ALL_PERMISSIONS.contains(permission)) {
                    throw new IllegalStateException(
                            "角色 " + role + " 包含未知权限码：" + permission
                    );
                }
                if (!permissionSet.add(permission)) {
                    throw new IllegalStateException(
                            "角色 " + role + " 包含重复权限码：" + permission
                    );
                }
            }
            loadedConfig.put(role, Collections.unmodifiableSet(permissionSet));
        });
        return Collections.unmodifiableMap(loadedConfig);
    }

    private void validateRequiredRoles(Set<String> configuredRoles) {
        Set<String> requiredRoles = Set.of(
                UserConstant.STUDENT_ROLE,
                UserConstant.TEACHER_ROLE,
                UserConstant.ADMIN_ROLE
        );
        for (String requiredRole : requiredRoles) {
            if (!configuredRoles.contains(requiredRole)) {
                throw new IllegalStateException("权限配置缺少角色：" + requiredRole);
            }
        }
    }
}
