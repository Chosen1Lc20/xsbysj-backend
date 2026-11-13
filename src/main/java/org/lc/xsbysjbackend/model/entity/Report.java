package org.lc.xsbysjbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文档记录表
 * @TableName report
 */
@TableName(value ="report")
@Data
public class Report {
    /**
     * 文档记录主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学生用户主键
     */
    private Long studentid;

    /**
     * 选题记录主键
     */
    private Long selectionid;

    /**
     * 课题主键
     */
    private Long topicid;

    /**
     * 指导教师主键
     */
    private Long teacherid;

    /**
     * 文档类型
     */
    private String documenttype;

    /**
     * 文件路径
     */
    private String filepath;

    /**
     * 文件记录主键
     */
    private Long filerecordid;

    /**
     * 当前文档版本号
     */
    private Integer version;

    /**
     * 是否为该文档类型的最新版本
     */
    @TableField("isLatest")
    private Boolean latest;

    /**
     * 提交时间
     */
    private Date submittime;

    /**
     * 审核状态
     */
    private String auditstatus;

    /**
     * 审核教师主键
     */
    private Long reviewerid;

    /**
     * 审核意见
     */
    private String auditcomment;

    /**
     * 审核时间
     */
    private Date reviewtime;

    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 编辑时间
     */
    private Date edittime;

    /**
     * 更新时间
     */
    private Date updatetime;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Report other = (Report) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getStudentid() == null ? other.getStudentid() == null : this.getStudentid().equals(other.getStudentid()))
            && (this.getSelectionid() == null ? other.getSelectionid() == null : this.getSelectionid().equals(other.getSelectionid()))
            && (this.getTopicid() == null ? other.getTopicid() == null : this.getTopicid().equals(other.getTopicid()))
            && (this.getTeacherid() == null ? other.getTeacherid() == null : this.getTeacherid().equals(other.getTeacherid()))
            && (this.getDocumenttype() == null ? other.getDocumenttype() == null : this.getDocumenttype().equals(other.getDocumenttype()))
            && (this.getFilepath() == null ? other.getFilepath() == null : this.getFilepath().equals(other.getFilepath()))
            && (this.getFilerecordid() == null ? other.getFilerecordid() == null : this.getFilerecordid().equals(other.getFilerecordid()))
            && (this.getVersion() == null ? other.getVersion() == null : this.getVersion().equals(other.getVersion()))
            && (this.getLatest() == null ? other.getLatest() == null : this.getLatest().equals(other.getLatest()))
            && (this.getSubmittime() == null ? other.getSubmittime() == null : this.getSubmittime().equals(other.getSubmittime()))
            && (this.getAuditstatus() == null ? other.getAuditstatus() == null : this.getAuditstatus().equals(other.getAuditstatus()))
            && (this.getReviewerid() == null ? other.getReviewerid() == null : this.getReviewerid().equals(other.getReviewerid()))
            && (this.getAuditcomment() == null ? other.getAuditcomment() == null : this.getAuditcomment().equals(other.getAuditcomment()))
            && (this.getReviewtime() == null ? other.getReviewtime() == null : this.getReviewtime().equals(other.getReviewtime()))
            && (this.getCreatetime() == null ? other.getCreatetime() == null : this.getCreatetime().equals(other.getCreatetime()))
            && (this.getEdittime() == null ? other.getEdittime() == null : this.getEdittime().equals(other.getEdittime()))
            && (this.getUpdatetime() == null ? other.getUpdatetime() == null : this.getUpdatetime().equals(other.getUpdatetime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getStudentid() == null) ? 0 : getStudentid().hashCode());
        result = prime * result + ((getSelectionid() == null) ? 0 : getSelectionid().hashCode());
        result = prime * result + ((getTopicid() == null) ? 0 : getTopicid().hashCode());
        result = prime * result + ((getTeacherid() == null) ? 0 : getTeacherid().hashCode());
        result = prime * result + ((getDocumenttype() == null) ? 0 : getDocumenttype().hashCode());
        result = prime * result + ((getFilepath() == null) ? 0 : getFilepath().hashCode());
        result = prime * result + ((getFilerecordid() == null) ? 0 : getFilerecordid().hashCode());
        result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
        result = prime * result + ((getLatest() == null) ? 0 : getLatest().hashCode());
        result = prime * result + ((getSubmittime() == null) ? 0 : getSubmittime().hashCode());
        result = prime * result + ((getAuditstatus() == null) ? 0 : getAuditstatus().hashCode());
        result = prime * result + ((getReviewerid() == null) ? 0 : getReviewerid().hashCode());
        result = prime * result + ((getAuditcomment() == null) ? 0 : getAuditcomment().hashCode());
        result = prime * result + ((getReviewtime() == null) ? 0 : getReviewtime().hashCode());
        result = prime * result + ((getCreatetime() == null) ? 0 : getCreatetime().hashCode());
        result = prime * result + ((getEdittime() == null) ? 0 : getEdittime().hashCode());
        result = prime * result + ((getUpdatetime() == null) ? 0 : getUpdatetime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", studentid=").append(studentid);
        sb.append(", selectionid=").append(selectionid);
        sb.append(", topicid=").append(topicid);
        sb.append(", teacherid=").append(teacherid);
        sb.append(", documenttype=").append(documenttype);
        sb.append(", filepath=").append(filepath);
        sb.append(", filerecordid=").append(filerecordid);
        sb.append(", version=").append(version);
        sb.append(", latest=").append(latest);
        sb.append(", submittime=").append(submittime);
        sb.append(", auditstatus=").append(auditstatus);
        sb.append(", reviewerid=").append(reviewerid);
        sb.append(", auditcomment=").append(auditcomment);
        sb.append(", reviewtime=").append(reviewtime);
        sb.append(", createtime=").append(createtime);
        sb.append(", edittime=").append(edittime);
        sb.append(", updatetime=").append(updatetime);
        sb.append("]");
        return sb.toString();
    }
}
