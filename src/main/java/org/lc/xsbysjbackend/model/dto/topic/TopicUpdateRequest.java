package org.lc.xsbysjbackend.model.dto.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑课题请求，字段为空时不修改。
 */
@Data
public class TopicUpdateRequest implements Serializable {

    private Long id;

    private String title;

    private String description;

    private String type;

    private Integer maxStudents;

    private static final long serialVersionUID = 1L;
}
