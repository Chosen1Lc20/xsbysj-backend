package org.lc.xsbysjbackend.model.dto.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 课题审核请求。
 */
@Data
public class TopicReviewRequest implements Serializable {

    private Long id;

    private Boolean approved;

    private String auditComment;

    private static final long serialVersionUID = 1L;
}
