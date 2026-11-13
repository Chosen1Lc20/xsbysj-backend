package org.lc.xsbysjbackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 答辩组学生信息。
 */
@Data
public class DefenseStudentVO implements Serializable {

    private Long id;

    private Long studentId;

    private String studentNo;

    private String studentName;

    private String studentDepartment;

    private Long selectionId;

    private Long topicId;

    private String topicTitle;

    private Long teacherId;

    private String teacherName;

    private static final long serialVersionUID = 1L;
}
