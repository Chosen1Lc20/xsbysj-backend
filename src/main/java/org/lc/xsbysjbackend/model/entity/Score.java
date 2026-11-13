package org.lc.xsbysjbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 成绩表
 * @TableName score
 */
@TableName(value ="score")
@Data
public class Score {
    /**
     * 成绩主键
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
     * 过程成绩
     */
    private BigDecimal processscore;

    /**
     * 答辩成绩
     */
    private BigDecimal defensescore;

    /**
     * 总评成绩
     */
    private BigDecimal totalscore;

    /**
     * 教师评语
     */
    private String teachercomment;

    /**
     * 录入时间
     */
    private Date entrytime;

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
        Score other = (Score) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getStudentid() == null ? other.getStudentid() == null : this.getStudentid().equals(other.getStudentid()))
            && (this.getSelectionid() == null ? other.getSelectionid() == null : this.getSelectionid().equals(other.getSelectionid()))
            && (this.getTopicid() == null ? other.getTopicid() == null : this.getTopicid().equals(other.getTopicid()))
            && (this.getTeacherid() == null ? other.getTeacherid() == null : this.getTeacherid().equals(other.getTeacherid()))
            && (this.getProcessscore() == null ? other.getProcessscore() == null : this.getProcessscore().equals(other.getProcessscore()))
            && (this.getDefensescore() == null ? other.getDefensescore() == null : this.getDefensescore().equals(other.getDefensescore()))
            && (this.getTotalscore() == null ? other.getTotalscore() == null : this.getTotalscore().equals(other.getTotalscore()))
            && (this.getTeachercomment() == null ? other.getTeachercomment() == null : this.getTeachercomment().equals(other.getTeachercomment()))
            && (this.getEntrytime() == null ? other.getEntrytime() == null : this.getEntrytime().equals(other.getEntrytime()))
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
        result = prime * result + ((getProcessscore() == null) ? 0 : getProcessscore().hashCode());
        result = prime * result + ((getDefensescore() == null) ? 0 : getDefensescore().hashCode());
        result = prime * result + ((getTotalscore() == null) ? 0 : getTotalscore().hashCode());
        result = prime * result + ((getTeachercomment() == null) ? 0 : getTeachercomment().hashCode());
        result = prime * result + ((getEntrytime() == null) ? 0 : getEntrytime().hashCode());
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
        sb.append(", processscore=").append(processscore);
        sb.append(", defensescore=").append(defensescore);
        sb.append(", totalscore=").append(totalscore);
        sb.append(", teachercomment=").append(teachercomment);
        sb.append(", entrytime=").append(entrytime);
        sb.append(", createtime=").append(createtime);
        sb.append(", edittime=").append(edittime);
        sb.append(", updatetime=").append(updatetime);
        sb.append("]");
        return sb.toString();
    }
}
