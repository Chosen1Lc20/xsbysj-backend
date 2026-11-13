package org.lc.xsbysjbackend.model.vo;

import lombok.Data;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;

import java.io.Serializable;
import java.util.Date;

/**
 * 课题信息。
 */
@Data
public class TopicVO implements Serializable {

    private Long id;

    private Long teacherId;

    private String teacherName;

    private String teacherDepartment;

    private String title;

    private String description;

    private String type;

    private Integer maxStudents;

    private Long selectedCount;

    private Integer remainingSlots;

    private String status;

    private Long reviewerId;

    private String auditComment;

    private Date reviewTime;

    private Date createTime;

    private Date editTime;

    private Date updateTime;

    public static TopicVO fromTopic(Topic topic, User teacher, long selectedCount) {
        TopicVO topicVO = new TopicVO();
        topicVO.setId(topic.getId());
        topicVO.setTeacherId(topic.getTeacherid());
        if (teacher != null) {
            topicVO.setTeacherName(teacher.getName());
            topicVO.setTeacherDepartment(teacher.getDepartment());
        }
        topicVO.setTitle(topic.getTitle());
        topicVO.setDescription(topic.getDescription());
        topicVO.setType(topic.getType());
        topicVO.setMaxStudents(topic.getMaxstudents());
        topicVO.setSelectedCount(selectedCount);
        topicVO.setRemainingSlots((int) Math.max(0, topic.getMaxstudents() - selectedCount));
        topicVO.setStatus(topic.getStatus());
        topicVO.setReviewerId(topic.getReviewerid());
        topicVO.setAuditComment(topic.getAuditcomment());
        topicVO.setReviewTime(topic.getReviewtime());
        topicVO.setCreateTime(topic.getCreatetime());
        topicVO.setEditTime(topic.getEdittime());
        topicVO.setUpdateTime(topic.getUpdatetime());
        return topicVO;
    }

    private static final long serialVersionUID = 1L;
}
