package org.lc.xsbysjbackend.model.vo;

import lombok.Data;
import org.lc.xsbysjbackend.model.entity.Score;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 毕业设计成绩信息。
 */
@Data
public class ScoreVO implements Serializable {

    private Long id;

    private Long studentId;

    private String studentNo;

    private String studentName;

    private String studentDepartment;

    private Long selectionId;

    private Long topicId;

    private String topicTitle;

    private Long teacherId;

    private String teacherName;

    private BigDecimal processScore;

    private BigDecimal defenseScore;

    private BigDecimal totalScore;

    private String teacherComment;

    private Date entryTime;

    private Date createTime;

    private Date editTime;

    private Date updateTime;

    public static ScoreVO fromScore(Score score, Selection selection, Topic topic,
                                    User student, User teacher) {
        ScoreVO scoreVO = new ScoreVO();
        scoreVO.setId(score.getId());
        scoreVO.setStudentId(score.getStudentid());
        if (student != null) {
            scoreVO.setStudentNo(student.getUserNo());
            scoreVO.setStudentName(student.getName());
            scoreVO.setStudentDepartment(student.getDepartment());
        }
        scoreVO.setSelectionId(score.getSelectionid());
        if (selection != null) {
            scoreVO.setSelectionId(selection.getId());
        }
        scoreVO.setTopicId(score.getTopicid());
        if (topic != null) {
            scoreVO.setTopicTitle(topic.getTitle());
        }
        scoreVO.setTeacherId(score.getTeacherid());
        if (teacher != null) {
            scoreVO.setTeacherName(teacher.getName());
        }
        scoreVO.setProcessScore(score.getProcessscore());
        scoreVO.setDefenseScore(score.getDefensescore());
        scoreVO.setTotalScore(score.getTotalscore());
        scoreVO.setTeacherComment(score.getTeachercomment());
        scoreVO.setEntryTime(score.getEntrytime());
        scoreVO.setCreateTime(score.getCreatetime());
        scoreVO.setEditTime(score.getEdittime());
        scoreVO.setUpdateTime(score.getUpdatetime());
        return scoreVO;
    }

    private static final long serialVersionUID = 1L;
}
