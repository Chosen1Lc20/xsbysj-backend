package org.lc.xsbysjbackend.model.dto.assignment;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 编辑任务书请求，字段为空时不修改。
 */
@Data
public class AssignmentUpdateRequest implements Serializable {

    private Long id;

    private String title;

    private String content;

    private Date deadline;

    private Long fileRecordId;

    private static final long serialVersionUID = 1L;
}
