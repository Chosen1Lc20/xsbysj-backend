package org.lc.xsbysjbackend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户注册请求。
 */
@Data
public class UserRegisterRequest implements Serializable {

    private String useraccount;

    private String password;

    private String confirmPassword;

    private String userNo;

    private String name;

    /**
     * 用户类型：student、teacher。
     */
    private String role;

    /**
     * 院系。
     */
    private String department;

    private static final long serialVersionUID = 1L;
}
