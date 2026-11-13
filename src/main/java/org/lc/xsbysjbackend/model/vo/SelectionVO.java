package org.lc.xsbysjbackend.model.vo;

import lombok.Data;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;

import java.io.Serializable;
import java.util.Date;

/**
 * 选题信息。
 */
@Data
public class SelectionVO implements Serializable {

    private Long id;

    private Long studentId;

    private String studentName;

    private String studentNo;

    private String studentDepartment;

    private Long topicId;

    private String topicTitle;

    private Long teacherId;

    private String teacherName;

    private Date selectionTime;

    private String status;

    private Long reviewerId;

    private String auditComment;

    private Date reviewTime;

    private Date createTime;

    private Date editTime;

    private Date updateTime;

    public static SelectionVO fromSelection(Selection selection, Topic topic,
                                            User student, User teacher) {
        SelectionVO selectionVO = new SelectionVO();
        selectionVO.setId(selection.getId());
        selectionVO.setStudentId(selection.getStudentid());
        if (student != null) {
            selectionVO.setStudentName(student.getName());
            selectionVO.setStudentNo(student.getUserNo());
            selectionVO.setStudentDepartment(student.getDepartment());
        }
        selectionVO.setTopicId(selection.getTopicid());
        if (topic != null) {
            selectionVO.setTopicTitle(topic.getTitle());
            selectionVO.setTeacherId(topic.getTeacherid());
        }
        if (teacher != null) {
            selectionVO.setTeacherName(teacher.getName());
        }
        selectionVO.setSelectionTime(selection.getSelectiontime());
        selectionVO.setStatus(selection.getStatus());
        selectionVO.setReviewerId(selection.getReviewerid());
        selectionVO.setAuditComment(selection.getAuditcomment());
        selectionVO.setReviewTime(selection.getReviewtime());
        selectionVO.setCreateTime(selection.getCreatetime());
        selectionVO.setEditTime(selection.getEdittime());
        selectionVO.setUpdateTime(selection.getUpdatetime());
        return selectionVO;
    }

    private static final long serialVersionUID = 1L;
}
