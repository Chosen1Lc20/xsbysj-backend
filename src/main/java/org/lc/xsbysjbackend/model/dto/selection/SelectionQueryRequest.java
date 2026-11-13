package org.lc.xsbysjbackend.model.dto.selection;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lc.xsbysjbackend.common.PageRequest;

import java.io.Serializable;

/**
 * 选题分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SelectionQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private Long topicId;

    private Long studentId;

    private String topicTitle;

    private String status;

    private static final long serialVersionUID = 1L;
}
