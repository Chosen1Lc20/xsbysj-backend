package org.lc.xsbysjbackend.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.common.BaseResponse;
import org.lc.xsbysjbackend.common.ResultUtils;
import org.lc.xsbysjbackend.constant.PermissionConstant;
import org.lc.xsbysjbackend.model.vo.FileUploadVO;
import org.lc.xsbysjbackend.service.FileRecordService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口。
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileRecordService fileRecordService;

    /**
     * 上传头像或毕业设计相关文档。
     */
    @SaCheckPermission(PermissionConstant.FILE_UPLOAD)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<FileUploadVO> uploadFile(@RequestPart("file") MultipartFile file) {
        long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(fileRecordService.uploadFile(file, userId));
    }

    /**
     * 编辑文件：上传新文件时替换原文件，否则修改文件名。
     */
    @SaCheckPermission(PermissionConstant.FILE_UPDATE)
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<FileUploadVO> updateFile(
            @RequestParam("id") Long id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName) {
        long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(fileRecordService.updateFile(id, file, fileName, userId));
    }
}
