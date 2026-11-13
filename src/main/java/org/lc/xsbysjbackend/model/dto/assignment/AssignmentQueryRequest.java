package org.lc.xsbysjbackend.model.dto.assignment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lc.xsbysjbackend.common.PageRequest;

import java.io.Serializable;

/**
 * 任务书分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssignmentQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private Long selectionId;

    private Long topicId;

    private Long studentId;

    private Long teacherId;

    private String title;

    private String status;

    private static final long serialVersionUID = 1L;
}
