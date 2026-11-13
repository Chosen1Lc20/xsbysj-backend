package org.lc.xsbysjbackend.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.common.BaseResponse;
import org.lc.xsbysjbackend.common.ResultUtils;
import org.lc.xsbysjbackend.constant.PermissionConstant;
import org.lc.xsbysjbackend.model.dto.document.DocumentQueryRequest;
import org.lc.xsbysjbackend.model.dto.document.DocumentReviewRequest;
import org.lc.xsbysjbackend.model.dto.document.DocumentSubmitRequest;
import org.lc.xsbysjbackend.model.vo.DocumentVO;
import org.lc.xsbysjbackend.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 过程文档接口。
 */
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final ReportService reportService;

    @SaCheckPermission(PermissionConstant.DOCUMENT_SUBMIT)
    @PostMapping("/submit")
    public BaseResponse<Long> submitDocument(
            @RequestBody DocumentSubmitRequest documentSubmitRequest) {
        return ResultUtils.success(reportService.submitDocument(documentSubmitRequest));
    }

    @SaCheckPermission(PermissionConstant.DOCUMENT_REVIEW)
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewDocument(
            @RequestBody DocumentReviewRequest documentReviewRequest) {
        return ResultUtils.success(reportService.reviewDocument(documentReviewRequest));
    }

    @SaCheckPermission(PermissionConstant.DOCUMENT_READ)
    @GetMapping("/get/vo")
    public BaseResponse<DocumentVO> getDocumentVoById(@RequestParam long id) {
        return ResultUtils.success(reportService.getDocumentVoById(id));
    }

    @SaCheckPermission(PermissionConstant.DOCUMENT_READ)
    @PostMapping("/list/page/vo")
    public BaseResponse<IPage<DocumentVO>> listDocumentVoByPage(
            @RequestBody DocumentQueryRequest documentQueryRequest) {
        return ResultUtils.success(
                reportService.listDocumentVoByPage(documentQueryRequest)
        );
    }
}
