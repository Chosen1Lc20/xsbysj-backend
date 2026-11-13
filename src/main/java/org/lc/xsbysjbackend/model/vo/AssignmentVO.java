package org.lc.xsbysjbackend.model.vo;

import lombok.Data;
import org.lc.xsbysjbackend.model.entity.Assignment;
import org.lc.xsbysjbackend.model.entity.FileRecord;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务书信息。
 */
@Data
public class AssignmentVO implements Serializable {

    private Long id;

    private Long selectionId;

    private Long studentId;

    private String studentName;

    private String studentNo;

    private String studentDepartment;

    private Long topicId;

    private String topicTitle;

    private Long teacherId;

    private String teacherName;

    private String title;

    private String content;

    private Long fileRecordId;

    private String fileName;

    private String fileUrl;

    private Date deadline;

    private String status;

    private Date issueTime;

    private Date createTime;

    private Date editTime;

    private Date updateTime;

    public static AssignmentVO fromAssignment(Assignment assignment, Selection selection,
                                              Topic topic, User student, User teacher,
                                              FileRecord fileRecord) {
        AssignmentVO assignmentVO = new AssignmentVO();
        assignmentVO.setId(assignment.getId());
        assignmentVO.setSelectionId(assignment.getSelectionid());
        if (selection != null) {
            assignmentVO.setStudentId(selection.getStudentid());
        }
        if (student != null) {
            assignmentVO.setStudentName(student.getName());
            assignmentVO.setStudentNo(student.getUserNo());
            assignmentVO.setStudentDepartment(student.getDepartment());
        }
        assignmentVO.setTopicId(assignment.getTopicid());
        if (topic != null) {
            assignmentVO.setTopicTitle(topic.getTitle());
        }
        assignmentVO.setTeacherId(assignment.getTeacherid());
        if (teacher != null) {
            assignmentVO.setTeacherName(teacher.getName());
        }
        assignmentVO.setTitle(assignment.getTitle());
        assignmentVO.setContent(assignment.getContent());
        assignmentVO.setFileRecordId(assignment.getFilerecordid());
        if (fileRecord != null) {
            assignmentVO.setFileName(fileRecord.getFileName());
            assignmentVO.setFileUrl(fileRecord.getFileUrl());
        }
        assignmentVO.setDeadline(assignment.getDeadline());
        assignmentVO.setStatus(assignment.getStatus());
        assignmentVO.setIssueTime(assignment.getIssuetime());
        assignmentVO.setCreateTime(assignment.getCreatetime());
        assignmentVO.setEditTime(assignment.getEdittime());
        assignmentVO.setUpdateTime(assignment.getUpdatetime());
        return assignmentVO;
    }

    private static final long serialVersionUID = 1L;
}
