package org.lc.xsbysjbackend.model.vo;

import lombok.Data;
import org.lc.xsbysjbackend.model.entity.FileRecord;
import org.lc.xsbysjbackend.model.entity.Report;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.model.enums.DocumentAuditStatusEnum;
import org.lc.xsbysjbackend.model.enums.DocumentTypeEnum;

import java.io.Serializable;
import java.util.Date;

/**
 * 过程文档信息。
 */
@Data
public class DocumentVO implements Serializable {

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

    private String documentType;

    private String documentTypeText;

    private Integer version;

    private Boolean latest;

    private Long fileRecordId;

    private String fileName;

    private String fileUrl;

    private String fileExtension;

    private Long fileSize;

    private Date submitTime;

    private String auditStatus;

    private String auditStatusText;

    private Long reviewerId;

    private String reviewerName;

    private String auditComment;

    private Date reviewTime;

    private Date createTime;

    private Date editTime;

    private Date updateTime;

    public static DocumentVO fromReport(Report report, Selection selection, Topic topic,
                                        User student, User teacher, User reviewer,
                                        FileRecord fileRecord) {
        DocumentVO documentVO = new DocumentVO();
        documentVO.setId(report.getId());
        documentVO.setSelectionId(report.getSelectionid());
        documentVO.setStudentId(report.getStudentid());
        if (student != null) {
            documentVO.setStudentName(student.getName());
            documentVO.setStudentNo(student.getUserNo());
            documentVO.setStudentDepartment(student.getDepartment());
        }
        if (selection != null) {
            documentVO.setSelectionId(selection.getId());
        }
        documentVO.setTopicId(report.getTopicid());
        if (topic != null) {
            documentVO.setTopicTitle(topic.getTitle());
        }
        documentVO.setTeacherId(report.getTeacherid());
        if (teacher != null) {
            documentVO.setTeacherName(teacher.getName());
        }
        documentVO.setDocumentType(report.getDocumenttype());
        DocumentTypeEnum documentType = DocumentTypeEnum.getByValue(report.getDocumenttype());
        if (documentType != null) {
            documentVO.setDocumentTypeText(documentType.getMessage());
        }
        documentVO.setVersion(report.getVersion());
        documentVO.setLatest(report.getLatest());
        documentVO.setFileRecordId(report.getFilerecordid());
        if (fileRecord != null) {
            documentVO.setFileName(fileRecord.getFileName());
            documentVO.setFileUrl(fileRecord.getFileUrl());
            documentVO.setFileExtension(fileRecord.getFileExtension());
            documentVO.setFileSize(fileRecord.getFileSize());
        }
        documentVO.setSubmitTime(report.getSubmittime());
        documentVO.setAuditStatus(report.getAuditstatus());
        DocumentAuditStatusEnum auditStatus =
                DocumentAuditStatusEnum.getByValue(report.getAuditstatus());
        if (auditStatus != null) {
            documentVO.setAuditStatusText(auditStatus.getMessage());
        }
        documentVO.setReviewerId(report.getReviewerid());
        if (reviewer != null) {
            documentVO.setReviewerName(reviewer.getName());
        }
        documentVO.setAuditComment(report.getAuditcomment());
        documentVO.setReviewTime(report.getReviewtime());
        documentVO.setCreateTime(report.getCreatetime());
        documentVO.setEditTime(report.getEdittime());
        documentVO.setUpdateTime(report.getUpdatetime());
        return documentVO;
    }

    private static final long serialVersionUID = 1L;
}
