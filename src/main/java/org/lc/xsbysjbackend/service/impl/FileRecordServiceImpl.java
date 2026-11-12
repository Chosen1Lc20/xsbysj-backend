package org.lc.xsbysjbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.exception.CosClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lc.xsbysjbackend.exception.BusinessException;
import org.lc.xsbysjbackend.exception.ErrorCode;
import org.lc.xsbysjbackend.manager.CosManager;
import org.lc.xsbysjbackend.mapper.FileRecordMapper;
import org.lc.xsbysjbackend.model.entity.FileRecord;
import org.lc.xsbysjbackend.model.enums.FileTypeEnum;
import org.lc.xsbysjbackend.model.vo.FileUploadVO;
import org.lc.xsbysjbackend.service.FileRecordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件记录服务实现，包含文件表和 COS 的业务操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecord>
        implements FileRecordService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final CosManager cosManager;

    @Override
    public FileUploadVO uploadFile(MultipartFile multipartFile, long uploadUserId) {
        validateLogin(uploadUserId);
        ResolvedFile resolvedFile = resolveFile(multipartFile);
        String objectKey = createObjectKey(resolvedFile.fileType(), uploadUserId, resolvedFile.extension());

        String fileUrl;
        try {
            fileUrl = cosManager.uploadFileToCOS(
                    multipartFile,
                    objectKey,
                    resolvedFile.contentType()
            );
        } catch (IOException | CosClientException e) {
            log.error("文件上传到 COS 失败，objectKey={}", objectKey, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败，请稍后重试");
        }

        FileRecord fileRecord = new FileRecord();
        fileRecord.setUploadUserId(uploadUserId);
        fileRecord.setFileKey(objectKey);
        fileRecord.setFileUrl(fileUrl);
        fileRecord.setFileType(resolvedFile.fileType().getValue());
        fileRecord.setFileExtension(resolvedFile.extension());
        fileRecord.setFileName(resolvedFile.fileName());
        fileRecord.setFileSize(multipartFile.getSize());
        fileRecord.setContentType(resolvedFile.contentType());

        try {
            if (!save(fileRecord)) {
                throw new IllegalStateException("文件记录保存失败");
            }
        } catch (RuntimeException e) {
            log.error("文件记录保存失败，开始删除 COS 文件，objectKey={}", objectKey, e);
            compensateDeleteObject(objectKey);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件保存失败，请稍后重试");
        }

        return toFileUploadVO(fileRecord);
    }

    @Override
    public FileUploadVO updateFile(Long id, MultipartFile multipartFile,
                                   String fileName, long uploadUserId) {
        validateLogin(uploadUserId);
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件主键不能为空");
        }

        FileRecord fileRecord = getById(id);
        if (fileRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }
        if (!fileRecord.getUploadUserId().equals(uploadUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权编辑其他用户上传的文件");
        }

        String normalizedFileName = normalizeFileName(fileName);
        boolean replaceFile = multipartFile != null && !multipartFile.isEmpty();
        if (!replaceFile && normalizedFileName == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新文件和新文件名不能同时为空");
        }

        if (!replaceFile) {
            fileRecord.setFileName(normalizedFileName);
            updateFileRecord(fileRecord, null);
            return toFileUploadVO(fileRecord);
        }

        ResolvedFile resolvedFile = resolveFile(multipartFile);
        if (!fileRecord.getFileType().equals(resolvedFile.fileType().getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "替换文件类型必须与原文件一致");
        }

        String newObjectKey = createObjectKey(
                resolvedFile.fileType(),
                uploadUserId,
                resolvedFile.extension()
        );
        String newFileUrl;
        try {
            newFileUrl = cosManager.uploadFileToCOS(
                    multipartFile,
                    newObjectKey,
                    resolvedFile.contentType()
            );
        } catch (IOException | CosClientException e) {
            log.error("替换文件上传到 COS 失败，objectKey={}", newObjectKey, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败，请稍后重试");
        }

        String oldObjectKey = fileRecord.getFileKey();
        fileRecord.setFileKey(newObjectKey);
        fileRecord.setFileUrl(newFileUrl);
        fileRecord.setFileExtension(resolvedFile.extension());
        fileRecord.setFileName(resolvedFile.fileName());
        fileRecord.setFileSize(multipartFile.getSize());
        fileRecord.setContentType(resolvedFile.contentType());

        updateFileRecord(fileRecord, newObjectKey);
        deleteObjectQuietly(oldObjectKey);
        return toFileUploadVO(fileRecord);
    }

    private ResolvedFile resolveFile(MultipartFile multipartFile) {
        validateFile(multipartFile);

        String fileName = StringUtils.getFilename(multipartFile.getOriginalFilename());
        validateFileName(fileName);

        String extension = StringUtils.getFilenameExtension(fileName);
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        String contentType = ALLOWED_CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "仅支持 jpg、jpeg、png、gif、webp、pdf、doc、docx 格式"
            );
        }

        FileTypeEnum fileType = IMAGE_EXTENSIONS.contains(extension)
                ? FileTypeEnum.AVATAR
                : FileTypeEnum.ATTACHMENT;
        return new ResolvedFile(fileName, extension, contentType, fileType);
    }

    private String createObjectKey(FileTypeEnum fileType, long uploadUserId, String extension) {
        String date = LocalDate.now(BUSINESS_ZONE).format(DATE_FORMATTER);
        return "%s/%d/%s/%s.%s".formatted(
                fileType.getValue(),
                uploadUserId,
                date,
                UUID.randomUUID().toString().replace("-", ""),
                extension
        );
    }

    private String normalizeFileName(String fileName) {
        String normalizedFileName = StringUtils.getFilename(
                StringUtils.trimWhitespace(fileName)
        );
        if (!StringUtils.hasText(normalizedFileName)) {
            return null;
        }
        validateFileName(normalizedFileName);
        return normalizedFileName;
    }

    private void validateFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能超过255个字符");
        }
    }

    private void validateLogin(long uploadUserId) {
        if (uploadUserId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态无效");
        }
    }

    private void updateFileRecord(FileRecord fileRecord, String newObjectKey) {
        try {
            if (!updateById(fileRecord)) {
                throw new IllegalStateException("文件记录更新失败");
            }
        } catch (RuntimeException e) {
            log.error("文件记录更新失败，fileId={}", fileRecord.getId(), e);
            if (newObjectKey != null) {
                deleteObjectQuietly(newObjectKey);
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件更新失败，请稍后重试");
        }
    }

    private FileUploadVO toFileUploadVO(FileRecord fileRecord) {
        return new FileUploadVO(
                fileRecord.getId(),
                fileRecord.getFileType(),
                fileRecord.getFileKey(),
                fileRecord.getFileUrl(),
                fileRecord.getFileName(),
                fileRecord.getFileExtension(),
                fileRecord.getContentType(),
                fileRecord.getFileSize()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件大小不能超过50MB");
        }
    }

    private void compensateDeleteObject(String objectKey) {
        deleteObjectQuietly(objectKey);
    }

    private void deleteObjectQuietly(String objectKey) {
        try {
            cosManager.deleteObject(objectKey);
        } catch (RuntimeException e) {
            log.error("删除 COS 文件失败，objectKey={}", objectKey, e);
        }
    }

    private record ResolvedFile(
            String fileName,
            String extension,
            String contentType,
            FileTypeEnum fileType
    ) {
    }

}
