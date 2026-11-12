package org.lc.xsbysjbackend.common;

import cn.hutool.crypto.digest.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 密码哈希与校验工具。
 */
public final class PasswordUtils {

    private static final String BCRYPT_PREFIX_2A = "$2a$";

    private static final String BCRYPT_PREFIX_2B = "$2b$";

    private static final String BCRYPT_PREFIX_2Y = "$2y$";

    private PasswordUtils() {
    }

    /**
     * 使用 BCrypt 加密密码。
     */
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    /**
     * 校验密码，兼容尚未迁移的旧明文密码。
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (!isEncoded(storedPassword)) {
            return MessageDigest.isEqual(
                    rawPassword.getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8)
            );
        }
        try {
            return BCrypt.checkpw(rawPassword, storedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断密码是否已经使用 BCrypt 存储。
     */
    public static boolean isEncoded(String password) {
        return password != null
                && (password.startsWith(BCRYPT_PREFIX_2A)
                || password.startsWith(BCRYPT_PREFIX_2B)
                || password.startsWith(BCRYPT_PREFIX_2Y));
    }
}
