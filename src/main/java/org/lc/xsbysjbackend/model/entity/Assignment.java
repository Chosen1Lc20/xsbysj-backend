package org.lc.xsbysjbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 任务书表
 * @TableName assignment
 */
@TableName(value ="assignment")
@Data
public class Assignment {
    /**
     * 任务书主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 选题记录主键
     */
    private Long selectionid;

    /**
     * 指导教师主键
     */
    private Long teacherid;

    /**
     * 课题主键
     */
    private Long topicid;

    /**
     * 任务书标题
     */
    private String title;

    /**
     * 任务书内容
     */
    private String content;

    /**
     * 附件路径
     */
    private String attachment;

    /**
     * 任务书附件文件记录主键
     */
    private Long filerecordid;

    /**
     * 任务截止时间
     */
    private Date deadline;

    /**
     * 任务书状态
     */
    private String status;

    /**
     * 下发时间
     */
    private Date issuetime;

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
        Assignment other = (Assignment) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getSelectionid() == null ? other.getSelectionid() == null : this.getSelectionid().equals(other.getSelectionid()))
            && (this.getTeacherid() == null ? other.getTeacherid() == null : this.getTeacherid().equals(other.getTeacherid()))
            && (this.getTopicid() == null ? other.getTopicid() == null : this.getTopicid().equals(other.getTopicid()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getAttachment() == null ? other.getAttachment() == null : this.getAttachment().equals(other.getAttachment()))
            && (this.getFilerecordid() == null ? other.getFilerecordid() == null : this.getFilerecordid().equals(other.getFilerecordid()))
            && (this.getDeadline() == null ? other.getDeadline() == null : this.getDeadline().equals(other.getDeadline()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getIssuetime() == null ? other.getIssuetime() == null : this.getIssuetime().equals(other.getIssuetime()))
            && (this.getCreatetime() == null ? other.getCreatetime() == null : this.getCreatetime().equals(other.getCreatetime()))
            && (this.getEdittime() == null ? other.getEdittime() == null : this.getEdittime().equals(other.getEdittime()))
            && (this.getUpdatetime() == null ? other.getUpdatetime() == null : this.getUpdatetime().equals(other.getUpdatetime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getSelectionid() == null) ? 0 : getSelectionid().hashCode());
        result = prime * result + ((getTeacherid() == null) ? 0 : getTeacherid().hashCode());
        result = prime * result + ((getTopicid() == null) ? 0 : getTopicid().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getAttachment() == null) ? 0 : getAttachment().hashCode());
        result = prime * result + ((getFilerecordid() == null) ? 0 : getFilerecordid().hashCode());
        result = prime * result + ((getDeadline() == null) ? 0 : getDeadline().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getIssuetime() == null) ? 0 : getIssuetime().hashCode());
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
        sb.append(", selectionid=").append(selectionid);
        sb.append(", teacherid=").append(teacherid);
        sb.append(", topicid=").append(topicid);
        sb.append(", title=").append(title);
        sb.append(", content=").append(content);
        sb.append(", attachment=").append(attachment);
        sb.append(", filerecordid=").append(filerecordid);
        sb.append(", deadline=").append(deadline);
        sb.append(", status=").append(status);
        sb.append(", issuetime=").append(issuetime);
        sb.append(", createtime=").append(createtime);
        sb.append(", edittime=").append(edittime);
        sb.append(", updatetime=").append(updatetime);
        sb.append("]");
        return sb.toString();
    }
}
