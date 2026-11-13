package org.lc.xsbysjbackend.model.dto.document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lc.xsbysjbackend.common.PageRequest;

import java.io.Serializable;

/**
 * 过程文档分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private Long selectionId;

    private Long topicId;

    private Long studentId;

    private Long teacherId;

    private String documentType;

    private String auditStatus;

    private Boolean latestOnly = true;

    private static final long serialVersionUID = 1L;
}
