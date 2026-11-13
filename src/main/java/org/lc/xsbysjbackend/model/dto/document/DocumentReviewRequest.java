package org.lc.xsbysjbackend.model.dto.document;

import lombok.Data;

import java.io.Serializable;

/**
 * 教师审核过程文档请求。
 */
@Data
public class DocumentReviewRequest implements Serializable {

    private Long id;

    private Boolean approved;

    private String auditComment;

    private static final long serialVersionUID = 1L;
}
