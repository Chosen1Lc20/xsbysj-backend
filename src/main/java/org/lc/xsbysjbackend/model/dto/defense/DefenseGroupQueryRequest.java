package org.lc.xsbysjbackend.model.dto.defense;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lc.xsbysjbackend.common.PageRequest;

import java.io.Serializable;
import java.util.Date;

/**
 * 答辩组分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DefenseGroupQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String groupName;

    private String location;

    private Long teacherId;

    private Long studentId;

    private Long selectionId;

    private Date defenseTimeStart;

    private Date defenseTimeEnd;

    private static final long serialVersionUID = 1L;
}
