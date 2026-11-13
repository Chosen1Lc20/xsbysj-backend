package org.lc.xsbysjbackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 答辩组教师信息。
 */
@Data
public class DefenseTeacherVO implements Serializable {

    private Long id;

    private Long teacherId;

    private String teacherNo;

    private String teacherName;

    private String teacherDepartment;

    private static final long serialVersionUID = 1L;
}
