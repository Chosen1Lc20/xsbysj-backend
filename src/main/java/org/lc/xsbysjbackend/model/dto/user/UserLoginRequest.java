package org.lc.xsbysjbackend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户登录请求。
 */
@Data
public class UserLoginRequest implements Serializable {

    private String useraccount;

    private String password;

    private static final long serialVersionUID = 1L;
}
