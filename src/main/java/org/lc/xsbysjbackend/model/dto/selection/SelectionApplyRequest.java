package org.lc.xsbysjbackend.model.dto.selection;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生选题申请。
 */
@Data
public class SelectionApplyRequest implements Serializable {

    private Long topicId;

    private static final long serialVersionUID = 1L;
}
