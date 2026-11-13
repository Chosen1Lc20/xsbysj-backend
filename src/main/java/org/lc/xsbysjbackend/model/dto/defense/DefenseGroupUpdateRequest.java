package org.lc.xsbysjbackend.model.dto.defense;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 编辑答辩组请求，字段为空时不修改。
 */
@Data
public class DefenseGroupUpdateRequest implements Serializable {

    private Long id;

    private String groupName;

    private Date defenseTime;

    private String location;

    private String remark;

    private List<Long> teacherIds;

    private List<Long> studentIds;

    private static final long serialVersionUID = 1L;
}
