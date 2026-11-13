package org.lc.xsbysjbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SelectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void studentCanApplyAndTeacherCanApproveSelection() throws Exception {
        LoginUser teacher = createAndLogin("selectionteacher01", "T20263001",
                UserConstant.TEACHER_ROLE);
        LoginUser student = createAndLogin("selectionstudent01", "S20263001",
                UserConstant.STUDENT_ROLE);
        long topicId = createApprovedTopic(teacher.id(), "选题审核通过测试", 1);

        long selectionId = applySelection(student.cookie(), topicId);

        mockMvc.perform(post("/api/selection/list/page/vo")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectionQueryBody(null, "PENDING", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].topicTitle")
                        .value("选题审核通过测试"));

        reviewSelection(teacher.cookie(), selectionId, true, "同意选题");

        mockMvc.perform(get("/api/selection/get/vo")
                        .param("id", Long.toString(selectionId))
                        .cookie(student.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.auditComment").value("同意选题"))
                .andExpect(jsonPath("$.data.teacherName").value("测试教师"))
                .andExpect(jsonPath("$.data.studentNo").value("S20263001"));

        mockMvc.perform(get("/api/topic/get/vo")
                        .param("id", Long.toString(topicId))
                        .cookie(student.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.maxStudents").value(1))
                .andExpect(jsonPath("$.data.selectedCount").value(1))
                .andExpect(jsonPath("$.data.remainingSlots").value(0));

        mockMvc.perform(post("/api/selection/apply")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d
                                }
                                """.formatted(topicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("已有待审核或已通过的选题"));
    }

    @Test
    void studentCanCancelAndReapplyButCannotReview() throws Exception {
        LoginUser teacher = createAndLogin("selectionteacher02", "T20263002",
                UserConstant.TEACHER_ROLE);
        LoginUser student = createAndLogin("selectionstudent02", "S20263002",
                UserConstant.STUDENT_ROLE);
        long topicId = createApprovedTopic(teacher.id(), "选题取消测试", 1);
        long selectionId = applySelection(student.cookie(), topicId);

        mockMvc.perform(post("/api/selection/review")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d,
                                  "approved": true
                                }
                                """.formatted(selectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权限执行该操作"));

        cancelSelection(student.cookie(), selectionId);
        long reappliedId = applySelection(student.cookie(), topicId);
        assertEquals(selectionId, reappliedId);

        mockMvc.perform(get("/api/selection/get/vo")
                        .param("id", Long.toString(reappliedId))
                        .cookie(student.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reviewTime").doesNotExist())
                .andExpect(jsonPath("$.data.auditComment").doesNotExist());

        mockMvc.perform(post("/api/selection/list/page/vo")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectionQueryBody(null, "PENDING", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void teacherCannotReviewOtherTeachersSelection() throws Exception {
        LoginUser ownerTeacher = createAndLogin("selectionteacher03", "T20263003",
                UserConstant.TEACHER_ROLE);
        LoginUser otherTeacher = createAndLogin("selectionteacher04", "T20263004",
                UserConstant.TEACHER_ROLE);
        LoginUser student = createAndLogin("selectionstudent03", "S20263003",
                UserConstant.STUDENT_ROLE);
        long topicId = createApprovedTopic(ownerTeacher.id(), "选题归属测试", 1);
        long selectionId = applySelection(student.cookie(), topicId);

        mockMvc.perform(post("/api/selection/review")
                        .cookie(otherTeacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(selectionId, true, "越权审核")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message")
                        .value("无权审核其他教师课题的选题"));
    }

    @Test
    void approvedCapacityCannotBeExceeded() throws Exception {
        LoginUser teacher = createAndLogin("selectionteacher05", "T20263005",
                UserConstant.TEACHER_ROLE);
        LoginUser studentOne = createAndLogin("selectionstudent04", "S20263004",
                UserConstant.STUDENT_ROLE);
        LoginUser studentTwo = createAndLogin("selectionstudent05", "S20263005",
                UserConstant.STUDENT_ROLE);
        long topicId = createApprovedTopic(teacher.id(), "选题名额测试", 1);
        long firstSelectionId = applySelection(studentOne.cookie(), topicId);
        long secondSelectionId = applySelection(studentTwo.cookie(), topicId);

        reviewSelection(teacher.cookie(), firstSelectionId, true, "通过第一名学生");

        mockMvc.perform(post("/api/selection/review")
                        .cookie(teacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(secondSelectionId, true, "通过第二名学生")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("课题名额已满"));
    }

    private long createApprovedTopic(long teacherId, String title, int maxStudents) {
        Topic topic = new Topic();
        topic.setTeacherid(teacherId);
        topic.setTitle(title);
        topic.setDescription("选题测试课题");
        topic.setType("软件开发");
        topic.setMaxstudents(maxStudents);
        topic.setStatus(TopicStatusEnum.APPROVED.name());
        topicService.save(topic);
        return topic.getId();
    }

    private long applySelection(Cookie token, long topicId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/selection/apply")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d
                                }
                                """.formatted(topicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return Long.parseLong(readData(result).asText());
    }

    private void cancelSelection(Cookie token, long selectionId) throws Exception {
        mockMvc.perform(post("/api/selection/cancel")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d
                                }
                                """.formatted(selectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    private void reviewSelection(Cookie token, long selectionId,
                                 boolean approved, String comment) throws Exception {
        mockMvc.perform(post("/api/selection/review")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewBody(selectionId, approved, comment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    private String reviewBody(long selectionId, boolean approved, String comment) {
        return """
                {
                  "id": %d,
                  "approved": %s,
                  "auditComment": "%s"
                }
                """.formatted(selectionId, approved, comment);
    }

    private String selectionQueryBody(Long topicId, String status, Long studentId) {
        String topicIdJson = topicId == null ? "null" : topicId.toString();
        String statusJson = status == null ? "null" : "\"" + status + "\"";
        String studentIdJson = studentId == null ? "null" : studentId.toString();
        return """
                {
                  "current": 1,
                  "pageSize": 10,
                  "topicId": %s,
                  "status": %s,
                  "studentId": %s
                }
                """.formatted(topicIdJson, statusJson, studentIdJson);
    }

    private LoginUser createAndLogin(String useraccount, String userNo, String role)
            throws Exception {
        UserAddRequest addRequest = new UserAddRequest();
        addRequest.setUseraccount(useraccount);
        addRequest.setPassword("123456");
        addRequest.setUserNo(userNo);
        addRequest.setName(UserConstant.TEACHER_ROLE.equals(role) ? "测试教师" : "测试学生");
        addRequest.setRole(role);
        addRequest.setDepartment("计算机学院");
        long userId = userService.addUser(addRequest);

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
