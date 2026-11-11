package org.lc.xsbysjbackend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 管理员新增用户请求。
 */
@Data
public class UserAddRequest implements Serializable {

    private String useraccount;

    private String password;

    private String userNo;

    private String name;

    private String role;

    private String dept;

    private String avatarUrl;

    private static final long serialVersionUID = 1L;
}
