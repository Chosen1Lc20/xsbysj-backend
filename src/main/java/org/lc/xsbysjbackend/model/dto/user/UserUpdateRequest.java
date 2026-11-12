package org.lc.xsbysjbackend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户编辑请求，字段为空时不修改对应数据。
 */
@Data
public class UserUpdateRequest implements Serializable {

    private Long id;

    private String useraccount;

    private String password;

    private String userNo;

    private String name;

    private String role;

    private String department;

    private String avatarUrl;

    private static final long serialVersionUID = 1L;
}
