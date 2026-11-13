package org.lc.xsbysjbackend.model.dto.score;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lc.xsbysjbackend.common.PageRequest;

import java.io.Serializable;

/**
 * 成绩分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScoreQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private Long selectionId;

    private Long topicId;

    private Long studentId;

    private Long teacherId;

    private static final long serialVersionUID = 1L;
}
