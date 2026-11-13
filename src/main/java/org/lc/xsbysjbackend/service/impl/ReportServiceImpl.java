package org.lc.xsbysjbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.exception.BusinessException;
import org.lc.xsbysjbackend.exception.ErrorCode;
import org.lc.xsbysjbackend.mapper.ReportMapper;
import org.lc.xsbysjbackend.model.dto.document.DocumentQueryRequest;
import org.lc.xsbysjbackend.model.dto.document.DocumentReviewRequest;
import org.lc.xsbysjbackend.model.dto.document.DocumentSubmitRequest;
import org.lc.xsbysjbackend.model.entity.FileRecord;
import org.lc.xsbysjbackend.model.entity.Report;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.model.enums.DocumentAuditStatusEnum;
import org.lc.xsbysjbackend.model.enums.DocumentTypeEnum;
import org.lc.xsbysjbackend.model.enums.FileTypeEnum;
import org.lc.xsbysjbackend.model.enums.SelectionStatusEnum;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.model.vo.DocumentVO;
import org.lc.xsbysjbackend.service.FileRecordService;
import org.lc.xsbysjbackend.service.ReportService;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author lianchao0921
* @description 针对表【report(文档记录表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report>
    implements ReportService {

    private static final int MAX_AUDIT_COMMENT_LENGTH = 1000;

    private final SelectionService selectionService;

    private final TopicService topicService;

    private final UserService userService;

    private final FileRecordService fileRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long submitDocument(DocumentSubmitRequest documentSubmitRequest) {
        if (documentSubmitRequest == null || documentSubmitRequest.getSelectionId() == null
                || documentSubmitRequest.getSelectionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选题主键不能为空");
        }
        DocumentTypeEnum documentType =
                validateDocumentType(documentSubmitRequest.getDocumentType());
        User student = getCurrentStudent();
        Selection selection = getSelectionOrThrow(documentSubmitRequest.getSelectionId());
        if (!student.getId().equals(selection.getStudentid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权为其他学生的选题提交文档");
        }
        if (!SelectionStatusEnum.APPROVED.name().equals(selection.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有审核通过的选题可以提交文档");
        }
        Topic topic = getTopicOrThrow(selection.getTopicid());
        if (!TopicStatusEnum.APPROVED.name().equals(topic.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "选题对应课题尚未审核通过");
        }
        FileRecord fileRecord = validateFile(
                documentSubmitRequest.getFileRecordId(),
                student.getId()
        );

        Report latestReport = getLatestReport(selection.getId(), documentType.name());
        if (latestReport != null) {
            if (DocumentAuditStatusEnum.PENDING.name().equals(latestReport.getAuditstatus())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前文档正在审核中，不能重复提交");
            }
            if (DocumentAuditStatusEnum.APPROVED.name().equals(latestReport.getAuditstatus())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前文档已审核通过，不能再次提交");
            }
            lambdaUpdate()
                    .eq(Report::getId, latestReport.getId())
                    .eq(Report::getLatest, true)
                    .set(Report::getLatest, false)
                    .update();
        }

        Date now = new Date();
        Report report = new Report();
        report.setStudentid(student.getId());
        report.setSelectionid(selection.getId());
        report.setTopicid(topic.getId());
        report.setTeacherid(topic.getTeacherid());
        report.setDocumenttype(documentType.name());
        report.setFilepath(fileRecord.getFileUrl());
        report.setFilerecordid(fileRecord.getId());
        report.setVersion(latestReport == null ? 1 : latestReport.getVersion() + 1);
        report.setLatest(true);
        report.setSubmittime(now);
        report.setAuditstatus(DocumentAuditStatusEnum.PENDING.name());
        try {
            if (!save(report)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交过程文档失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文档版本冲突，请刷新后重试");
        }
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewDocument(DocumentReviewRequest documentReviewRequest) {
        if (documentReviewRequest == null || documentReviewRequest.getId() == null
                || documentReviewRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档主键不能为空");
        }
        if (documentReviewRequest.getApproved() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核结果不能为空");
        }
        User teacher = getCurrentTeacher();
        Report report = getReportOrThrow(documentReviewRequest.getId());
        if (!teacher.getId().equals(report.getTeacherid())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权审核其他教师课题的文档");
        }
        if (!DocumentAuditStatusEnum.PENDING.name().equals(report.getAuditstatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有待审核文档可以审核");
        }

        String auditComment = normalizeNullable(documentReviewRequest.getAuditComment());
        validateMaxLength(auditComment, MAX_AUDIT_COMMENT_LENGTH, "审核意见不能超过1000个字符");
        if (!documentReviewRequest.getApproved() && StrUtil.isBlank(auditComment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "驳回文档时必须填写审核意见");
        }

        boolean updated = lambdaUpdate()
                .eq(Report::getId, report.getId())
                .eq(Report::getTeacherid, teacher.getId())
                .eq(Report::getAuditstatus, DocumentAuditStatusEnum.PENDING.name())
                .set(Report::getAuditstatus, documentReviewRequest.getApproved()
                        ? DocumentAuditStatusEnum.APPROVED.name()
                        : DocumentAuditStatusEnum.REJECTED.name())
                .set(Report::getReviewerid, teacher.getId())
                .set(Report::getAuditcomment, auditComment)
                .set(Report::getReviewtime, new Date())
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "审核过程文档失败");
        }
        return true;
    }

    @Override
    public DocumentVO getDocumentVoById(long id) {
        Report report = getReportOrThrow(id);
        checkViewPermission(report, userService.getLoginUser());
        return toDocumentVoList(List.of(report)).get(0);
    }

    @Override
    public IPage<DocumentVO> listDocumentVoByPage(DocumentQueryRequest documentQueryRequest) {
        validatePageRequest(documentQueryRequest);
        User currentUser = userService.getLoginUser();
        LambdaQueryWrapper<Report> queryWrapper = new LambdaQueryWrapper<>();
        applyRoleScope(queryWrapper, currentUser);

        queryWrapper.eq(documentQueryRequest.getId() != null,
                Report::getId, documentQueryRequest.getId());
        queryWrapper.eq(documentQueryRequest.getSelectionId() != null,
                Report::getSelectionid, documentQueryRequest.getSelectionId());
        queryWrapper.eq(documentQueryRequest.getTopicId() != null,
                Report::getTopicid, documentQueryRequest.getTopicId());
        queryWrapper.eq(documentQueryRequest.getStudentId() != null,
                Report::getStudentid, documentQueryRequest.getStudentId());
        queryWrapper.eq(documentQueryRequest.getTeacherId() != null,
                Report::getTeacherid, documentQueryRequest.getTeacherId());
        queryWrapper.eq(StrUtil.isNotBlank(documentQueryRequest.getDocumentType()),
                Report::getDocumenttype, documentQueryRequest.getDocumentType());
        queryWrapper.eq(StrUtil.isNotBlank(documentQueryRequest.getAuditStatus()),
                Report::getAuditstatus, documentQueryRequest.getAuditStatus());
        if (!Boolean.FALSE.equals(documentQueryRequest.getLatestOnly())) {
            queryWrapper.eq(Report::getLatest, true);
        }
        applyDocumentSort(
                queryWrapper,
                documentQueryRequest.getSortField(),
                documentQueryRequest.getSortOrder()
        );

        Page<Report> reportPage = page(
                new Page<>(documentQueryRequest.getCurrent(), documentQueryRequest.getPageSize()),
                queryWrapper
        );
        Page<DocumentVO> voPage = new Page<>(
                reportPage.getCurrent(),
                reportPage.getSize(),
                reportPage.getTotal()
        );
        voPage.setRecords(toDocumentVoList(reportPage.getRecords()));
        return voPage;
    }

    private void applyRoleScope(LambdaQueryWrapper<Report> queryWrapper, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)) {
            return;
        }
        if (UserConstant.TEACHER_ROLE.equals(role)) {
            queryWrapper.eq(Report::getTeacherid, currentUser.getId());
            return;
        }
        if (UserConstant.STUDENT_ROLE.equals(role)) {
            queryWrapper.eq(Report::getStudentid, currentUser.getId());
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查询过程文档");
    }

    private void checkViewPermission(Report report, User currentUser) {
        String role = currentUser.getRole();
        if (UserConstant.ADMIN_ROLE.equals(role)
                || (UserConstant.TEACHER_ROLE.equals(role)
                && currentUser.getId().equals(report.getTeacherid()))
                || (UserConstant.STUDENT_ROLE.equals(role)
                && currentUser.getId().equals(report.getStudentid()))) {
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该过程文档");
    }

    private Report getLatestReport(long selectionId, String documentType) {
        return lambdaQuery()
                .eq(Report::getSelectionid, selectionId)
                .eq(Report::getDocumenttype, documentType)
                .orderByDesc(Report::getVersion)
                .last("LIMIT 1")
                .one();
    }

    private List<DocumentVO> toDocumentVoList(List<Report> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        Set<Long> selectionIds = reports.stream()
                .map(Report::getSelectionid)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> topicIds = reports.stream()
                .map(Report::getTopicid)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> fileRecordIds = reports.stream()
                .map(Report::getFilerecordid)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, Selection> selectionMap = selectionIds.isEmpty()
                ? Collections.emptyMap()
                : selectionService.listByIds(selectionIds).stream()
                .collect(Collectors.toMap(Selection::getId, Function.identity()));
        Map<Long, Topic> topicMap = topicIds.isEmpty()
                ? Collections.emptyMap()
                : topicService.listByIds(topicIds).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
        Map<Long, FileRecord> fileRecordMap = fileRecordIds.isEmpty()
                ? Collections.emptyMap()
                : fileRecordService.listByIds(fileRecordIds).stream()
                .collect(Collectors.toMap(FileRecord::getId, Function.identity()));

        Set<Long> userIds = new LinkedHashSet<>();
        reports.stream().map(Report::getStudentid).filter(id -> id != null).forEach(userIds::add);
        reports.stream().map(Report::getTeacherid).filter(id -> id != null).forEach(userIds::add);
        reports.stream().map(Report::getReviewerid).filter(id -> id != null).forEach(userIds::add);
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return reports.stream()
                .map(report -> DocumentVO.fromReport(
                        report,
                        selectionMap.get(report.getSelectionid()),
                        topicMap.get(report.getTopicid()),
                        userMap.get(report.getStudentid()),
                        userMap.get(report.getTeacherid()),
                        userMap.get(report.getReviewerid()),
                        fileRecordMap.get(report.getFilerecordid())
                ))
                .toList();
    }

    private FileRecord validateFile(Long fileRecordId, Long studentId) {
        if (fileRecordId == null || fileRecordId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过程文档必须上传文件");
        }
        FileRecord fileRecord = fileRecordService.getById(fileRecordId);
        if (fileRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "过程文档文件不存在");
        }
        if (!studentId.equals(fileRecord.getUploadUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能使用本人上传的过程文档文件");
        }
        if (!FileTypeEnum.ATTACHMENT.getValue().equals(fileRecord.getFileType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过程文档必须是文档附件");
        }
        return fileRecord;
    }

    private DocumentTypeEnum validateDocumentType(String documentType) {
        DocumentTypeEnum documentTypeEnum = DocumentTypeEnum.getByValue(documentType);
        if (documentTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过程文档类型不合法");
        }
        return documentTypeEnum;
    }

    private Report getReportOrThrow(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过程文档主键不合法");
        }
        Report report = getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "过程文档不存在");
        }
        return report;
    }

    private Selection getSelectionOrThrow(long id) {
        Selection selection = selectionService.getById(id);
        if (selection == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "选题记录不存在");
        }
        return selection;
    }

    private Topic getTopicOrThrow(long id) {
        Topic topic = topicService.getById(id);
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课题不存在");
        }
        return topic;
    }

    private User getCurrentStudent() {
        User user = userService.getLoginUser();
        if (!UserConstant.STUDENT_ROLE.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可以提交过程文档");
        }
        return user;
    }

    private User getCurrentTeacher() {
        User user = userService.getLoginUser();
        if (!UserConstant.TEACHER_ROLE.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可以审核过程文档");
        }
        return user;
    }

    private void validatePageRequest(DocumentQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        if (request.getCurrent() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1到50之间");
        }
        if (StrUtil.isNotBlank(request.getDocumentType())
                && DocumentTypeEnum.getByValue(request.getDocumentType()) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过程文档类型不合法");
        }
        if (StrUtil.isNotBlank(request.getAuditStatus())
                && DocumentAuditStatusEnum.getByValue(request.getAuditStatus()) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态不合法");
        }
    }

    private void validateMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, message);
        }
    }

    private static String normalizeNullable(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private static void applyDocumentSort(LambdaQueryWrapper<Report> queryWrapper,
                                          String sortField, String sortOrder) {
        boolean ascending = "ascend".equalsIgnoreCase(sortOrder);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByDesc(Report::getSubmittime);
            return;
        }
        switch (sortField) {
            case "id" -> queryWrapper.orderBy(true, ascending, Report::getId);
            case "documentType" -> queryWrapper.orderBy(
                    true, ascending, Report::getDocumenttype
            );
            case "version" -> queryWrapper.orderBy(true, ascending, Report::getVersion);
            case "auditStatus" -> queryWrapper.orderBy(
                    true, ascending, Report::getAuditstatus
            );
            case "submitTime" -> queryWrapper.orderBy(
                    true, ascending, Report::getSubmittime
            );
            case "reviewTime" -> queryWrapper.orderBy(
                    true, ascending, Report::getReviewtime
            );
            case "createTime" -> queryWrapper.orderBy(
                    true, ascending, Report::getCreatetime
            );
            case "updateTime" -> queryWrapper.orderBy(
                    true, ascending, Report::getUpdatetime
            );
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序字段不合法");
        }
    }
}
