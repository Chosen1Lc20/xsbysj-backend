package org.lc.xsbysjbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 选题表
 * @TableName selection
 */
@TableName(value ="selection")
@Data
public class Selection {
    /**
     * 选题主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学生用户主键
     */
    private Long studentid;

    /**
     * 课题主键
     */
    private Long topicid;

    /**
     * 选题时间
     */
    private Date selectiontime;

    /**
     * 状态
     */
    private String status;

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
        Selection other = (Selection) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getStudentid() == null ? other.getStudentid() == null : this.getStudentid().equals(other.getStudentid()))
            && (this.getTopicid() == null ? other.getTopicid() == null : this.getTopicid().equals(other.getTopicid()))
            && (this.getSelectiontime() == null ? other.getSelectiontime() == null : this.getSelectiontime().equals(other.getSelectiontime()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
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
        result = prime * result + ((getTopicid() == null) ? 0 : getTopicid().hashCode());
        result = prime * result + ((getSelectiontime() == null) ? 0 : getSelectiontime().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
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
        sb.append(", topicid=").append(topicid);
        sb.append(", selectiontime=").append(selectiontime);
        sb.append(", status=").append(status);
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
