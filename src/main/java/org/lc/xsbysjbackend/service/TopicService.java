package org.lc.xsbysjbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.lc.xsbysjbackend.model.dto.topic.TopicAddRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicQueryRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicReviewRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicUpdateRequest;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.vo.TopicVO;

/**
* @author lianchao0921
* @description 针对表【topic(课题表)】的数据库操作Service
* @createDate 2026-11-11 15:19:31
*/
public interface TopicService extends IService<Topic> {

    /**
     * 教师新增课题草稿。
     */
    long addTopic(TopicAddRequest topicAddRequest);

    /**
     * 教师编辑自己的课题。
     */
    boolean updateTopic(TopicUpdateRequest topicUpdateRequest);

    /**
     * 教师删除自己的课题草稿或驳回课题。
     */
    boolean deleteTopic(long id);

    /**
     * 教师提交课题审核。
     */
    boolean submitTopicReview(long id);

    /**
     * 管理员审核课题。
     */
    boolean reviewTopic(TopicReviewRequest topicReviewRequest);

    /**
     * 根据主键查询课题。
     */
    TopicVO getTopicVoById(long id);

    /**
     * 根据当前用户角色分页查询课题。
     */
    IPage<TopicVO> listTopicVoByPage(TopicQueryRequest topicQueryRequest);
}
