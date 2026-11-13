package org.lc.xsbysjbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherCanCreateSubmitAndAdminApproveTopic() throws Exception {
        Cookie teacherToken = createAndLogin("topicteacher01", "T20262001",
                UserConstant.TEACHER_ROLE);
        Cookie adminToken = createAndLogin("topicadmin01", "A20262001",
                UserConstant.ADMIN_ROLE);
        Cookie studentToken = createAndLogin("topicstudent01", "S20262001",
                UserConstant.STUDENT_ROLE);

        String topicId = createTopic(teacherToken, "主题流程测试课题", "软件开发", 2);

        mockMvc.perform(post("/api/topic/list/page/vo")
                        .cookie(studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topicQueryBody("主题流程测试课题", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        submitTopic(teacherToken, topicId);

        mockMvc.perform(post("/api/topic/list/page/vo")
                        .cookie(adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topicQueryBody("主题流程测试课题", null, "PENDING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.records[0].maxStudents").value(2))
                .andExpect(jsonPath("$.data.records[0].selectedCount").value(0))
                .andExpect(jsonPath("$.data.records[0].remainingSlots").value(2));

        reviewTopic(adminToken, topicId, true, "审核通过");

        mockMvc.perform(post("/api/topic/list/page/vo")
                        .cookie(studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topicQueryBody("主题流程测试课题", "软件开发", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.data.records[0].teacherName").value("课题教师"))
                .andExpect(jsonPath("$.data.records[0].teacherDepartment").value("计算机学院"))
                .andExpect(jsonPath("$.data.records[0].selectedCount").value(0))
                .andExpect(jsonPath("$.data.records[0].remainingSlots").value(2));

        mockMvc.perform(get("/api/topic/get/vo")
                        .param("id", topicId)
                        .cookie(adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.auditComment").value("审核通过"))
                .andExpect(jsonPath("$.data.reviewerId").exists())
                .andExpect(jsonPath("$.data.reviewTime").exists());

        String updateBody = """
                {
                  "id": "%s",
                  "title": "审核后不可修改"
                }
                """.formatted(topicId);
        mockMvc.perform(post("/api/topic/update")
                        .cookie(teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("当前课题状态不允许编辑或提交"));
    }

    @Test
    void teacherCanOnlyOperateOwnTopicAndStudentCannotCreateTopic() throws Exception {
        Cookie ownerToken = createAndLogin("topicteacher02", "T20262002",
                UserConstant.TEACHER_ROLE);
        Cookie otherTeacherToken = createAndLogin("topicteacher03", "T20262003",
                UserConstant.TEACHER_ROLE);
        Cookie studentToken = createAndLogin("topicstudent02", "S20262002",
                UserConstant.STUDENT_ROLE);
        String topicId = createTopic(ownerToken, "权限测试课题", "工程设计", 1);

        String updateBody = """
                {
                  "id": "%s",
                  "title": "越权修改"
                }
                """.formatted(topicId);
        mockMvc.perform(post("/api/topic/update")
                        .cookie(otherTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权操作其他教师的课题"));

        String addBody = """
                {
                  "title": "学生创建的课题",
                  "type": "软件开发",
                  "maxStudents": 1
                }
                """;
        mockMvc.perform(post("/api/topic/add")
                        .cookie(studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void rejectedTopicRequiresCommentAndCanBeEditedAndResubmitted() throws Exception {
        Cookie teacherToken = createAndLogin("topicteacher04", "T20262004",
                UserConstant.TEACHER_ROLE);
        Cookie adminToken = createAndLogin("topicadmin02", "A20262002",
                UserConstant.ADMIN_ROLE);
        String topicId = createTopic(teacherToken, "驳回流程测试课题", "算法研究", 1);
        submitTopic(teacherToken, topicId);

        String rejectWithoutCommentBody = """
                {
                  "id": "%s",
                  "approved": false
                }
                """.formatted(topicId);
        mockMvc.perform(post("/api/topic/review")
                        .cookie(adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectWithoutCommentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("驳回课题时必须填写审核意见"));

        reviewTopic(adminToken, topicId, false, "课题范围需要进一步明确");

        String updateBody = """
                {
                  "id": "%s",
                  "description": "已补充课题研究范围和预期成果"
                }
                """.formatted(topicId);
        mockMvc.perform(post("/api/topic/update")
                        .cookie(teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        submitTopic(teacherToken, topicId);

        mockMvc.perform(post("/api/topic/list/page/vo")
                        .cookie(teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topicQueryBody("驳回流程测试课题", null, "PENDING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("PENDING"));
    }

    private String createTopic(Cookie token, String title, String type, int maxStudents)
            throws Exception {
        String body = """
                {
                  "title": "%s",
                  "description": "课题测试简介",
                  "type": "%s",
                  "maxStudents": %d
                }
                """.formatted(title, type, maxStudents);
        MvcResult result = mockMvc.perform(post("/api/topic/add")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readData(result).asText();
    }

    private void submitTopic(Cookie token, String topicId) throws Exception {
        String body = """
                {
                  "id": "%s"
                }
                """.formatted(topicId);
        mockMvc.perform(post("/api/topic/submit")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    private void reviewTopic(Cookie token, String topicId, boolean approved, String comment)
            throws Exception {
        String body = """
                {
                  "id": "%s",
                  "approved": %s,
                  "auditComment": "%s"
                }
                """.formatted(topicId, approved, comment);
        mockMvc.perform(post("/api/topic/review")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    private String topicQueryBody(String title, String type, String status) {
        String typeJson = type == null ? "null" : "\"" + type + "\"";
        String statusJson = status == null ? "null" : "\"" + status + "\"";
        return """
                {
                  "current": 1,
                  "pageSize": 10,
                  "title": "%s",
                  "type": %s,
                  "status": %s
                }
                """.formatted(title, typeJson, statusJson);
    }

    private Cookie createAndLogin(String useraccount, String userNo, String role)
            throws Exception {
        UserAddRequest addRequest = new UserAddRequest();
        addRequest.setUseraccount(useraccount);
        addRequest.setPassword("123456");
        addRequest.setUserNo(userNo);
        addRequest.setName(UserConstant.TEACHER_ROLE.equals(role) ? "课题教师" : "课题用户");
        addRequest.setRole(role);
        addRequest.setDepartment("计算机学院");
        userService.addUser(addRequest);

        String loginBody = """
                {
                  "useraccount": "%s",
                  "password": "123456"
                }
                """.formatted(useraccount);
        MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Cookie token = findCookie(loginResult.getResponse().getCookies(), "satoken");
        assertNotNull(token);
        return token;
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
}
