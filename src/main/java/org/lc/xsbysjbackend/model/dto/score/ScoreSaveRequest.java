package org.lc.xsbysjbackend.model.dto.score;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 指导教师录入成绩请求。
 */
@Data
public class ScoreSaveRequest implements Serializable {

    private Long studentId;

    private BigDecimal processScore;

    private BigDecimal defenseScore;

    private String teacherComment;

    private static final long serialVersionUID = 1L;
}
