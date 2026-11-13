package org.lc.xsbysjbackend.model.dto.assignment;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 新增任务书请求。
 */
@Data
public class AssignmentAddRequest implements Serializable {

    private Long selectionId;

    private String title;

    private String content;

    private Date deadline;

    private Long fileRecordId;

    private static final long serialVersionUID = 1L;
}
