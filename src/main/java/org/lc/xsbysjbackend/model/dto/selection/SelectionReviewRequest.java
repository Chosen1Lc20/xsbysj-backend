package org.lc.xsbysjbackend.model.dto.selection;

import lombok.Data;

import java.io.Serializable;

/**
 * 教师审核选题请求。
 */
@Data
public class SelectionReviewRequest implements Serializable {

    private Long id;

    private Boolean approved;

    private String auditComment;

    private static final long serialVersionUID = 1L;
}
