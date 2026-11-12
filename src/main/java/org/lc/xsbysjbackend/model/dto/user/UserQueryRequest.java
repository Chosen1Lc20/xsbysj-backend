package org.lc.xsbysjbackend.model.dto.user;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lc.xsbysjbackend.common.PageRequest;

/**
 * 用户分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String useraccount;

    private String userNo;

    private String name;

    private String role;

    private String department;

    private static final long serialVersionUID = 1L;
}
