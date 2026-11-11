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

    private static final long serialVersionUID = 1L;
}
