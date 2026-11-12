package org.lc.xsbysjbackend.model.vo;

import java.io.Serializable;
import lombok.Data;
import org.lc.xsbysjbackend.model.entity.User;

/**
 * 脱敏后的用户信息。
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String useraccount;

    private String userNo;

    private String name;

    private String role;

    private String department;

    private String avatarUrl;

    public static UserVO fromUser(User user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUseraccount(user.getUseraccount());
        userVO.setUserNo(user.getUserNo());
        userVO.setName(user.getName());
        userVO.setRole(user.getRole());
        userVO.setDepartment(user.getDepartment());
        userVO.setAvatarUrl(user.getAvatarUrl());
        return userVO;
    }

    private static final long serialVersionUID = 1L;
}
