package org.lc.xsbysjbackend.model.dto.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增课题请求。
 */
@Data
public class TopicAddRequest implements Serializable {

    private String title;

    private String description;

    private String type;

    private Integer maxStudents;

    private static final long serialVersionUID = 1L;
}
