package org.lc.xsbysjbackend.model.vo;

import lombok.Data;
import org.lc.xsbysjbackend.model.entity.User;

import java.io.Serializable;

/**
 * 教师筛选选项。
 */
@Data
public class TeacherOptionVO implements Serializable {

    private Long id;

    private String userNo;

    private String name;

    private String department;

    public static TeacherOptionVO fromUser(User user) {
        TeacherOptionVO teacherOptionVO = new TeacherOptionVO();
        teacherOptionVO.setId(user.getId());
        teacherOptionVO.setUserNo(user.getUserNo());
        teacherOptionVO.setName(user.getName());
        teacherOptionVO.setDepartment(user.getDepartment());
        return teacherOptionVO;
    }

    private static final long serialVersionUID = 1L;
}
