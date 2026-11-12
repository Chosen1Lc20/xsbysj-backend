package org.lc.xsbysjbackend.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.common.BaseResponse;
import org.lc.xsbysjbackend.common.DeleteRequest;
import org.lc.xsbysjbackend.common.ResultUtils;
import org.lc.xsbysjbackend.constant.PermissionConstant;
import org.lc.xsbysjbackend.exception.BusinessException;
import org.lc.xsbysjbackend.exception.ErrorCode;
import org.lc.xsbysjbackend.model.dto.topic.TopicAddRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicQueryRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicReviewRequest;
import org.lc.xsbysjbackend.model.dto.topic.TopicUpdateRequest;
import org.lc.xsbysjbackend.model.vo.TopicVO;
import org.lc.xsbysjbackend.service.TopicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课题接口。
 */
@RestController
@RequestMapping("/api/topic")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @SaCheckPermission(PermissionConstant.TOPIC_ADD)
    @PostMapping("/add")
    public BaseResponse<Long> addTopic(@RequestBody TopicAddRequest topicAddRequest) {
        return ResultUtils.success(topicService.addTopic(topicAddRequest));
    }

    @SaCheckPermission(PermissionConstant.TOPIC_UPDATE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTopic(@RequestBody TopicUpdateRequest topicUpdateRequest) {
        return ResultUtils.success(topicService.updateTopic(topicUpdateRequest));
    }

    @SaCheckPermission(PermissionConstant.TOPIC_DELETE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTopic(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课题主键不能为空");
        }
        return ResultUtils.success(topicService.deleteTopic(deleteRequest.getId()));
    }

    @SaCheckPermission(PermissionConstant.TOPIC_SUBMIT)
    @PostMapping("/submit")
    public BaseResponse<Boolean> submitTopicReview(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课题主键不能为空");
        }
        return ResultUtils.success(topicService.submitTopicReview(deleteRequest.getId()));
    }

    @SaCheckPermission(PermissionConstant.TOPIC_REVIEW)
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewTopic(@RequestBody TopicReviewRequest topicReviewRequest) {
        return ResultUtils.success(topicService.reviewTopic(topicReviewRequest));
    }

    @SaCheckPermission(PermissionConstant.TOPIC_READ)
    @GetMapping("/get/vo")
    public BaseResponse<TopicVO> getTopicVoById(@RequestParam long id) {
        return ResultUtils.success(topicService.getTopicVoById(id));
    }

    @SaCheckPermission(PermissionConstant.TOPIC_READ)
    @PostMapping("/list/page/vo")
    public BaseResponse<IPage<TopicVO>> listTopicVoByPage(
            @RequestBody TopicQueryRequest topicQueryRequest) {
        return ResultUtils.success(topicService.listTopicVoByPage(topicQueryRequest));
    }
}
