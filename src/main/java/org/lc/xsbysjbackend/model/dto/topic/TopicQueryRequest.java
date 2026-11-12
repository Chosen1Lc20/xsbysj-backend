package org.lc.xsbysjbackend.model.dto.topic;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lc.xsbysjbackend.common.PageRequest;

import java.io.Serializable;

/**
 * 课题分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TopicQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private Long teacherId;

    private String title;

    private String type;

    private String status;

    private static final long serialVersionUID = 1L;
}
