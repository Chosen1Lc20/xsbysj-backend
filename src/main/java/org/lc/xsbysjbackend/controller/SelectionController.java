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
import org.lc.xsbysjbackend.model.dto.selection.SelectionApplyRequest;
import org.lc.xsbysjbackend.model.dto.selection.SelectionQueryRequest;
import org.lc.xsbysjbackend.model.dto.selection.SelectionReviewRequest;
import org.lc.xsbysjbackend.model.vo.SelectionVO;
import org.lc.xsbysjbackend.service.SelectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 选题接口。
 */
@RestController
@RequestMapping("/api/selection")
@RequiredArgsConstructor
public class SelectionController {

    private final SelectionService selectionService;

    @SaCheckPermission(PermissionConstant.SELECTION_APPLY)
    @PostMapping("/apply")
    public BaseResponse<Long> applySelection(
            @RequestBody SelectionApplyRequest selectionApplyRequest) {
        return ResultUtils.success(selectionService.applySelection(selectionApplyRequest));
    }

    @SaCheckPermission(PermissionConstant.SELECTION_CANCEL)
    @PostMapping("/cancel")
    public BaseResponse<Boolean> cancelSelection(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选题主键不能为空");
        }
        return ResultUtils.success(selectionService.cancelSelection(deleteRequest.getId()));
    }

    @SaCheckPermission(PermissionConstant.SELECTION_REVIEW)
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewSelection(
            @RequestBody SelectionReviewRequest selectionReviewRequest) {
        return ResultUtils.success(selectionService.reviewSelection(selectionReviewRequest));
    }

    @SaCheckPermission(PermissionConstant.SELECTION_READ)
    @GetMapping("/get/vo")
    public BaseResponse<SelectionVO> getSelectionVoById(@RequestParam long id) {
        return ResultUtils.success(selectionService.getSelectionVoById(id));
    }

    @SaCheckPermission(PermissionConstant.SELECTION_READ)
    @PostMapping("/list/page/vo")
    public BaseResponse<IPage<SelectionVO>> listSelectionVoByPage(
            @RequestBody SelectionQueryRequest selectionQueryRequest) {
        return ResultUtils.success(
                selectionService.listSelectionVoByPage(selectionQueryRequest)
        );
    }
}
