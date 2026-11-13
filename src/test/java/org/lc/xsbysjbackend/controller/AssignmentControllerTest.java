package org.lc.xsbysjbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.enums.SelectionStatusEnum;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.service.TopicService;
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
class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private SelectionService selectionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherCanCreateEditAndIssueAssignment() throws Exception {
        LoginUser teacher = createAndLogin("assignmentteacher01", "T20264001",
                UserConstant.TEACHER_ROLE, "任务书教师");
        LoginUser student = createAndLogin("assignmentstudent01", "S20264001",
                UserConstant.STUDENT_ROLE, "任务书学生");
        SelectionSetup setup = createApprovedSelection(
                teacher.id(), student.id(), "任务书完整流程课题"
        );

        String assignmentId = addAssignment(
                teacher.cookie(),
                setup.selectionId(),
                "毕业设计任务书",
                "完成需求分析、系统设计、编码、测试和论文撰写。"
        );

        mockMvc.perform(post("/api/assignment/list/page/vo")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentQueryBody("毕业设计任务书", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/assignment/get/vo")
                        .param("id", assignmentId)
                        .cookie(student.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("任务书尚未下发"));

        mockMvc.perform(post("/api/assignment/update")
                        .cookie(teacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "content": "完成系统开发、测试、部署和毕业论文。",
                                  "deadline": "2099-05-01 00:00:00"
                                }
                                """.formatted(assignmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        issueAssignment(teacher.cookie(), assignmentId);

        mockMvc.perform(get("/api/assignment/get/vo")
                        .param("id", assignmentId)
                        .cookie(student.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.issueTime").exists())
                .andExpect(jsonPath("$.data.deadline").value("2099-05-01 00:00:00"))
                .andExpect(jsonPath("$.data.studentName").value("任务书学生"))
                .andExpect(jsonPath("$.data.teacherName").value("任务书教师"))
                .andExpect(jsonPath("$.data.topicTitle").value("任务书完整流程课题"))
                .andExpect(jsonPath("$.data.content")
                        .value("完成系统开发、测试、部署和毕业论文。"));

        mockMvc.perform(post("/api/assignment/list/page/vo")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentQueryBody(null, "ISSUED", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(assignmentId));

        mockMvc.perform(post("/api/assignment/update")
                        .cookie(teacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "title": "下发后修改标题"
                                }
                                """.formatted(assignmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("只有草稿任务书可以执行该操作"));
    }

    @Test
    void teacherCanOnlyOperateOwnAssignmentsAndSelectionHasOnlyOneAssignment()
            throws Exception {
        LoginUser owner = createAndLogin("assignmentteacher02", "T20264002",
                UserConstant.TEACHER_ROLE, "任务书归属教师");
        LoginUser otherTeacher = createAndLogin("assignmentteacher03", "T20264003",
                UserConstant.TEACHER_ROLE, "其他任务书教师");
        LoginUser student = createAndLogin("assignmentstudent02", "S20264002",
                UserConstant.STUDENT_ROLE, "任务书归属学生");
        SelectionSetup setup = createApprovedSelection(
                owner.id(), student.id(), "任务书归属测试课题"
        );

        mockMvc.perform(post("/api/assignment/add")
                        .cookie(otherTeacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(setup.selectionId(), "越权创建任务书", "测试内容")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message")
                        .value("无权为其他教师课题下的选题创建任务书"));

        String assignmentId = addAssignment(
                owner.cookie(), setup.selectionId(), "任务书归属", "测试内容"
        );

        mockMvc.perform(post("/api/assignment/add")
                        .cookie(owner.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(setup.selectionId(), "重复任务书", "重复测试内容")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("该选题已创建任务书"));

        mockMvc.perform(post("/api/assignment/issue")
                        .cookie(otherTeacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s"
                                }
                                """.formatted(assignmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权操作其他教师的课题任务书"));

        mockMvc.perform(post("/api/assignment/delete")
                        .cookie(owner.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s"
                                }
                                """.formatted(assignmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/assignment/get/vo")
                        .param("id", assignmentId)
                        .cookie(owner.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("任务书不存在"));
    }

    @Test
    void adminCanViewAllButStudentsCannotViewOtherStudentsAssignments() throws Exception {
        LoginUser teacherOne = createAndLogin("assignmentteacher04", "T20264004",
                UserConstant.TEACHER_ROLE, "任务书教师一");
        LoginUser studentOne = createAndLogin("assignmentstudent03", "S20264003",
                UserConstant.STUDENT_ROLE, "任务书学生一");
        LoginUser teacherTwo = createAndLogin("assignmentteacher05", "T20264005",
                UserConstant.TEACHER_ROLE, "任务书教师二");
        LoginUser studentTwo = createAndLogin("assignmentstudent04", "S20264004",
                UserConstant.STUDENT_ROLE, "任务书学生二");
        LoginUser admin = createAndLogin("assignmentadmin01", "A20264001",
                UserConstant.ADMIN_ROLE, "任务书管理员");

        SelectionSetup setupOne = createApprovedSelection(
                teacherOne.id(), studentOne.id(), "任务书管理员查询课题一"
        );
        SelectionSetup setupTwo = createApprovedSelection(
                teacherTwo.id(), studentTwo.id(), "任务书管理员查询课题二"
        );
        String assignmentOne = addAssignment(
                teacherOne.cookie(), setupOne.selectionId(), "管理员查询任务书一", "任务书内容一"
        );
        String assignmentTwo = addAssignment(
                teacherTwo.cookie(), setupTwo.selectionId(), "管理员查询任务书二", "任务书内容二"
        );
        issueAssignment(teacherOne.cookie(), assignmentOne);
        issueAssignment(teacherTwo.cookie(), assignmentTwo);

        mockMvc.perform(get("/api/assignment/get/vo")
                        .param("id", assignmentTwo)
                        .cookie(studentOne.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权查看该任务书"));

        mockMvc.perform(post("/api/assignment/list/page/vo")
                        .cookie(studentOne.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentQueryBody("管理员查询任务书", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(assignmentOne));

        mockMvc.perform(post("/api/assignment/list/page/vo")
                        .cookie(admin.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentQueryBody("管理员查询任务书", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/assignment/get/vo")
                        .param("id", assignmentTwo)
                        .cookie(admin.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.teacherId")
                        .value(Long.toString(teacherTwo.id())))
                .andExpect(jsonPath("$.data.studentId")
                        .value(Long.toString(studentTwo.id())));
    }

    private SelectionSetup createApprovedSelection(long teacherId, long studentId,
                                                   String topicTitle) {
        Topic topic = new Topic();
        topic.setTeacherid(teacherId);
        topic.setTitle(topicTitle);
        topic.setDescription("任务书测试课题");
        topic.setType("软件开发");
        topic.setMaxstudents(1);
        topic.setStatus(TopicStatusEnum.APPROVED.name());
        topicService.save(topic);

        Selection selection = new Selection();
        selection.setStudentid(studentId);
        selection.setTopicid(topic.getId());
        selection.setStatus(SelectionStatusEnum.APPROVED.name());
        selectionService.save(selection);
        return new SelectionSetup(topic.getId(), selection.getId());
    }

    private String addAssignment(Cookie token, long selectionId, String title, String content)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/assignment/add")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(selectionId, title, content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readData(result).asText();
    }

    private String addBody(long selectionId, String title, String content) {
        return """
                {
                  "selectionId": "%d",
                  "title": "%s",
                  "content": "%s",
                  "deadline": "2099-05-01 00:00:00"
                }
                """.formatted(selectionId, title, content);
    }

    private void issueAssignment(Cookie token, String assignmentId) throws Exception {
        mockMvc.perform(post("/api/assignment/issue")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s"
                                }
                                """.formatted(assignmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    private String assignmentQueryBody(String title, String status, Long studentId) {
        String titleJson = title == null ? "null" : "\"" + title + "\"";
        String statusJson = status == null ? "null" : "\"" + status + "\"";
        String studentIdJson = studentId == null ? "null" : "\"" + studentId + "\"";
        return """
                {
                  "current": 1,
                  "pageSize": 10,
                  "title": %s,
                  "status": %s,
                  "studentId": %s
                }
                """.formatted(titleJson, statusJson, studentIdJson);
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

    private record SelectionSetup(long topicId, long selectionId) {
    }
}
