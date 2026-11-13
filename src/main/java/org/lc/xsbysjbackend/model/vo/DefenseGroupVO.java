package org.lc.xsbysjbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 答辩组信息。
 */
@Data
public class DefenseGroupVO implements Serializable {

    private Long id;

    private String groupName;

    private Date defenseTime;

    private String location;

    private String remark;

    private List<DefenseTeacherVO> teachers;

    private List<DefenseStudentVO> students;

    private Date createTime;

    private Date editTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
