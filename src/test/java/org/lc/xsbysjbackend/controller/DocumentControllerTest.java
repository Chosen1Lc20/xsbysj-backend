package org.lc.xsbysjbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.model.entity.FileRecord;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.enums.DocumentTypeEnum;
import org.lc.xsbysjbackend.model.enums.FileTypeEnum;
import org.lc.xsbysjbackend.model.enums.SelectionStatusEnum;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.service.FileRecordService;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private SelectionService selectionService;

    @Autowired
    private FileRecordService fileRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rejectedDocumentCanBeResubmittedAndApprovedDocumentIsLocked() throws Exception {
        LoginUser teacher = createAndLogin("documentteacher01", "T20265001",
                UserConstant.TEACHER_ROLE, "过程文档教师");
        LoginUser student = createAndLogin("documentstudent01", "S20265001",
                UserConstant.STUDENT_ROLE, "过程文档学生");
        long selectionId = createApprovedSelection(
                teacher.id(), student.id(), "过程文档流程测试课题"
        );

        long firstFileId = createFileRecord(student.id(), "开题报告V1.docx");
        String firstDocumentId = submitDocument(
                student.cookie(),
                selectionId,
                DocumentTypeEnum.OPENING_REPORT,
                firstFileId
        );

        mockMvc.perform(get("/api/document/get/vo")
                        .param("id", firstDocumentId)
                        .cookie(student.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.documentType").value("OPENING_REPORT"))
                .andExpect(jsonPath("$.data.documentTypeText").value("开题报告"))
                .andExpect(jsonPath("$.data.auditStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.latest").value(true))
                .andExpect(jsonPath("$.data.fileName").value("开题报告V1.docx"))
                .andExpect(jsonPath("$.data.teacherName").value("过程文档教师"));

        mockMvc.perform(post("/api/document/submit")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(selectionId, DocumentTypeEnum.OPENING_REPORT,
                                createFileRecord(student.id(), "重复开题报告.docx"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("当前文档正在审核中，不能重复提交"));

        reviewDocument(
                teacher.cookie(),
                firstDocumentId,
                false,
                "请补充研究背景和技术路线"
        );

        long secondFileId = createFileRecord(student.id(), "开题报告V2.docx");
        String secondDocumentId = submitDocument(
                student.cookie(),
                selectionId,
                DocumentTypeEnum.OPENING_REPORT,
                secondFileId
        );
        assertNotEquals(firstDocumentId, secondDocumentId);

        mockMvc.perform(post("/api/document/list/page/vo")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody(selectionId, null, true, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(secondDocumentId))
                .andExpect(jsonPath("$.data.records[0].version").value(2));

        mockMvc.perform(post("/api/document/list/page/vo")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody(selectionId, null, false, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));

        reviewDocument(teacher.cookie(), secondDocumentId, true, "开题报告审核通过");

        mockMvc.perform(post("/api/document/submit")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(selectionId, DocumentTypeEnum.OPENING_REPORT,
                                createFileRecord(student.id(), "开题报告V3.docx"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("当前文档已审核通过，不能再次提交"));
    }

    @Test
    void documentAccessAndReviewAreRestrictedByRoleAndTeacherOwnership() throws Exception {
        LoginUser ownerTeacher = createAndLogin("documentteacher02", "T20265002",
                UserConstant.TEACHER_ROLE, "过程文档归属教师");
        LoginUser otherTeacher = createAndLogin("documentteacher03", "T20265003",
                UserConstant.TEACHER_ROLE, "过程文档其他教师");
        LoginUser ownerStudent = createAndLogin("documentstudent02", "S20265002",
                UserConstant.STUDENT_ROLE, "过程文档归属学生");
        LoginUser otherStudent = createAndLogin("documentstudent03", "S20265003",
                UserConstant.STUDENT_ROLE, "过程文档其他学生");
        LoginUser admin = createAndLogin("documentadmin01", "A20265001",
                UserConstant.ADMIN_ROLE, "过程文档管理员");
        long selectionId = createApprovedSelection(
                ownerTeacher.id(), ownerStudent.id(), "过程文档权限测试课题"
        );
        long fileId = createFileRecord(ownerStudent.id(), "中期检查.docx");

        mockMvc.perform(post("/api/document/submit")
                        .cookie(otherStudent.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(selectionId, DocumentTypeEnum.MIDTERM_CHECK, fileId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权为其他学生的选题提交文档"));

        String documentId = submitDocument(
                ownerStudent.cookie(),
                selectionId,
                DocumentTypeEnum.MIDTERM_CHECK,
                fileId
        );

        mockMvc.perform(post("/api/document/review")
                        .cookie(otherTeacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(documentId, true, "越权审核")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权审核其他教师课题的文档"));

        mockMvc.perform(post("/api/document/review")
                        .cookie(ownerStudent.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(documentId, true, "学生审核")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权限执行该操作"));

        mockMvc.perform(get("/api/document/get/vo")
                        .param("id", documentId)
                        .cookie(otherStudent.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权查看该过程文档"));

        mockMvc.perform(post("/api/document/list/page/vo")
                        .cookie(otherTeacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody(selectionId, null, true, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/api/document/list/page/vo")
                        .cookie(admin.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody(selectionId, null, true, ownerStudent.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].studentName").value("过程文档归属学生"));
    }

    private long createApprovedSelection(long teacherId, long studentId, String topicTitle) {
        Topic topic = new Topic();
        topic.setTeacherid(teacherId);
        topic.setTitle(topicTitle);
        topic.setDescription("过程文档测试课题");
        topic.setType("软件开发");
        topic.setMaxstudents(1);
        topic.setStatus(TopicStatusEnum.APPROVED.name());
        topicService.save(topic);

        Selection selection = new Selection();
        selection.setStudentid(studentId);
        selection.setTopicid(topic.getId());
        selection.setStatus(SelectionStatusEnum.APPROVED.name());
        selectionService.save(selection);
        return selection.getId();
    }

    private long createFileRecord(long studentId, String fileName) {
        String uniqueKey = UUID.randomUUID().toString().replace("-", "");
        FileRecord fileRecord = new FileRecord();
        fileRecord.setUploadUserId(studentId);
        fileRecord.setFileKey("attachment/%d/%s.docx".formatted(studentId, uniqueKey));
        fileRecord.setFileUrl("https://example.com/attachment/%s.docx".formatted(uniqueKey));
        fileRecord.setFileType(FileTypeEnum.ATTACHMENT.getValue());
        fileRecord.setFileExtension("docx");
        fileRecord.setFileName(fileName);
        fileRecord.setFileSize(1024L);
        fileRecord.setContentType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        fileRecordService.save(fileRecord);
        return fileRecord.getId();
    }

    private String submitDocument(Cookie cookie, long selectionId,
                                  DocumentTypeEnum documentType, long fileRecordId)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/document/submit")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(selectionId, documentType, fileRecordId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readData(result).asText();
    }

    private String submitBody(long selectionId, DocumentTypeEnum documentType,
                              long fileRecordId) {
        return """
                {
                  "selectionId": "%d",
                  "documentType": "%s",
                  "fileRecordId": "%d"
                }
                """.formatted(selectionId, documentType.name(), fileRecordId);
    }

    private void reviewDocument(Cookie cookie, String documentId,
                                boolean approved, String auditComment) throws Exception {
        mockMvc.perform(post("/api/document/review")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(documentId, approved, auditComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    private String reviewBody(String documentId, boolean approved, String auditComment) {
        return """
                {
                  "id": "%s",
                  "approved": %s,
                  "auditComment": "%s"
                }
                """.formatted(documentId, approved, auditComment);
    }

    private String queryBody(long selectionId, String auditStatus,
                             boolean latestOnly, Long studentId) {
        String auditStatusJson = auditStatus == null ? "null" : "\"" + auditStatus + "\"";
        String studentIdJson = studentId == null ? "null" : "\"" + studentId + "\"";
        return """
                {
                  "current": 1,
                  "pageSize": 10,
                  "selectionId": "%d",
                  "auditStatus": %s,
                  "latestOnly": %s,
                  "studentId": %s
                }
                """.formatted(selectionId, auditStatusJson, latestOnly, studentIdJson);
    }

    private LoginUser createAndLogin(String useraccount, String userNo, String role, String name)
            throws Exception {
        UserAddRequest addRequest = new UserAddRequest();
        addRequest.setUseraccount(useraccount);
        addRequest.setPassword("123456");
        addRequest.setUserNo(userNo);
        addRequest.setName(name);
        addRequest.setRole(role);
        addRequest.setDepartment("计算机学院");
        long userId = userService.addUser(addRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "useraccount": "%s",
                                  "password": "123456"
                                }
                                """.formatted(useraccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Cookie token = findCookie(loginResult.getResponse().getCookies(), "satoken");
        assertNotNull(token);
        return new LoginUser(userId, token);
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private Cookie findCookie(Cookie[] cookies, String name) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    private record LoginUser(long id, Cookie cookie) {
    }
}
