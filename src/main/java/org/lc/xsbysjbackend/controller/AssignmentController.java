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
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentAddRequest;
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentQueryRequest;
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentUpdateRequest;
import org.lc.xsbysjbackend.model.vo.AssignmentVO;
import org.lc.xsbysjbackend.service.AssignmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务书接口。
 */
@RestController
@RequestMapping("/api/assignment")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @SaCheckPermission(PermissionConstant.ASSIGNMENT_ADD)
    @PostMapping("/add")
    public BaseResponse<Long> addAssignment(
            @RequestBody AssignmentAddRequest assignmentAddRequest) {
        return ResultUtils.success(
                assignmentService.addAssignment(assignmentAddRequest)
        );
    }

    @SaCheckPermission(PermissionConstant.ASSIGNMENT_UPDATE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateAssignment(
            @RequestBody AssignmentUpdateRequest assignmentUpdateRequest) {
        return ResultUtils.success(
                assignmentService.updateAssignment(assignmentUpdateRequest)
        );
    }

    @SaCheckPermission(PermissionConstant.ASSIGNMENT_DELETE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteAssignment(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务书主键不能为空");
        }
        return ResultUtils.success(assignmentService.deleteAssignment(deleteRequest.getId()));
    }

    @SaCheckPermission(PermissionConstant.ASSIGNMENT_ISSUE)
    @PostMapping("/issue")
    public BaseResponse<Boolean> issueAssignment(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务书主键不能为空");
        }
        return ResultUtils.success(assignmentService.issueAssignment(deleteRequest.getId()));
    }

    @SaCheckPermission(PermissionConstant.ASSIGNMENT_READ)
    @GetMapping("/get/vo")
    public BaseResponse<AssignmentVO> getAssignmentVoById(@RequestParam long id) {
        return ResultUtils.success(assignmentService.getAssignmentVoById(id));
    }

    @SaCheckPermission(PermissionConstant.ASSIGNMENT_READ)
    @PostMapping("/list/page/vo")
    public BaseResponse<IPage<AssignmentVO>> listAssignmentVoByPage(
            @RequestBody AssignmentQueryRequest assignmentQueryRequest) {
        return ResultUtils.success(
                assignmentService.listAssignmentVoByPage(assignmentQueryRequest)
        );
    }
}
