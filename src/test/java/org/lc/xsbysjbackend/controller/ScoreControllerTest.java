package org.lc.xsbysjbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.mapper.DefenseGroupStudentMapper;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.model.entity.DefenseGroup;
import org.lc.xsbysjbackend.model.entity.DefenseGroupStudent;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.model.enums.SelectionStatusEnum;
import org.lc.xsbysjbackend.model.enums.TopicStatusEnum;
import org.lc.xsbysjbackend.service.DefenseGroupService;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private SelectionService selectionService;

    @Autowired
    private DefenseGroupService defenseGroupService;

    @Autowired
    private DefenseGroupStudentMapper defenseGroupStudentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherCanSaveAndUpdateScoreAndAuthorizedUsersCanQueryIt() throws Exception {
        LoginUser teacher = createAndLogin("scoreteacher01", "T20267001",
                UserConstant.TEACHER_ROLE, "成绩教师");
        LoginUser student = createAndLogin("scorestudent01", "S20267001",
                UserConstant.STUDENT_ROLE, "成绩学生");
        LoginUser admin = createAndLogin("scoreadmin01", "A20267001",
                UserConstant.ADMIN_ROLE, "成绩管理员");
        SelectionSetup setup = createApprovedSelection(
                teacher.id(), student.id(), "成绩录入测试课题"
        );
        assignToDefenseGroup(
                student.id(),
                setup.selectionId(),
                "成绩答辩组一"
        );

        MvcResult saveResult = mockMvc.perform(post("/api/score/save")
                        .cookie(teacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(student.id(), "85.5", "90.0", "表现优秀")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.processScore").value(85.5))
                .andExpect(jsonPath("$.data.defenseScore").value(90.0))
                .andExpect(jsonPath("$.data.totalScore").value(88.2))
                .andExpect(jsonPath("$.data.teacherComment").value("表现优秀"))
                .andExpect(jsonPath("$.data.studentName").value("成绩学生"))
                .andExpect(jsonPath("$.data.teacherName").value("成绩教师"))
                .andExpect(jsonPath("$.data.topicTitle").value("成绩录入测试课题"))
                .andReturn();
        String scoreId = readData(saveResult).path("id").asText();

        mockMvc.perform(get("/api/score/my")
                        .cookie(student.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(scoreId))
                .andExpect(jsonPath("$.data.totalScore").value(88.2));

        mockMvc.perform(post("/api/score/list/page/vo")
                        .cookie(teacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody(student.id(), teacher.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(scoreId));

        mockMvc.perform(post("/api/score/list/page/vo")
                        .cookie(admin.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody(student.id(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        MvcResult updateResult = mockMvc.perform(post("/api/score/save")
                        .cookie(teacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(student.id(), "90.0", "80.0", "答辩表现良好")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalScore").value(84.0))
                .andExpect(jsonPath("$.data.teacherComment").value("答辩表现良好"))
                .andReturn();
        assertEquals(scoreId, readData(updateResult).path("id").asText());
    }

    @Test
    void onlyAdvisorCanEnterScoreAndStudentMustBeAssignedToDefenseGroup()
            throws Exception {
        LoginUser advisor = createAndLogin("scoreteacher02", "T20267002",
                UserConstant.TEACHER_ROLE, "成绩指导教师");
        LoginUser otherTeacher = createAndLogin("scoreteacher03", "T20267003",
                UserConstant.TEACHER_ROLE, "其他成绩教师");
        LoginUser student = createAndLogin("scorestudent02", "S20267002",
                UserConstant.STUDENT_ROLE, "成绩归属学生");
        LoginUser unassignedStudent = createAndLogin(
                "scorestudent03", "S20267003",
                UserConstant.STUDENT_ROLE, "未答辩学生"
        );
        LoginUser otherStudent = createAndLogin("scorestudent04", "S20267004",
                UserConstant.STUDENT_ROLE, "其他成绩学生");
        SelectionSetup setup = createApprovedSelection(
                advisor.id(), student.id(), "成绩权限测试课题"
        );
        createApprovedSelection(
                advisor.id(), unassignedStudent.id(), "未分配答辩课题"
        );
        assignToDefenseGroup(
                student.id(),
                setup.selectionId(),
                "成绩答辩组二"
        );

        mockMvc.perform(post("/api/score/save")
                        .cookie(otherTeacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(student.id(), "80.0", "80.0", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("只能录入本人课题学生的成绩"));

        mockMvc.perform(post("/api/score/save")
                        .cookie(advisor.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(
                                unassignedStudent.id(),
                                "80.0",
                                "80.0",
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("学生尚未分配答辩组，不能录入成绩"));

        MvcResult result = mockMvc.perform(post("/api/score/save")
                        .cookie(advisor.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(student.id(), "100.0", "0.0", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalScore").value(40.0))
                .andReturn();
        String scoreId = readData(result).path("id").asText();

        mockMvc.perform(post("/api/score/save")
                        .cookie(student.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(student.id(), "80.0", "80.0", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权限执行该操作"));

        mockMvc.perform(get("/api/score/get/vo")
                        .param("id", scoreId)
                        .cookie(otherStudent.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权查看该成绩"));
    }

    private SelectionSetup createApprovedSelection(long teacherId, long studentId,
                                                   String topicTitle) {
        Topic topic = new Topic();
        topic.setTeacherid(teacherId);
        topic.setTitle(topicTitle);
        topic.setDescription("成绩管理测试课题");
        topic.setType("软件开发");
        topic.setMaxstudents(1);
        topic.setStatus(TopicStatusEnum.APPROVED.name());
        topicService.save(topic);

        Selection selection = new Selection();
        selection.setStudentid(studentId);
        selection.setTopicid(topic.getId());
        selection.setSelectiontime(new Date());
        selection.setStatus(SelectionStatusEnum.APPROVED.name());
        selectionService.save(selection);
        return new SelectionSetup(topic.getId(), selection.getId());
    }

    private void assignToDefenseGroup(long studentId, long selectionId, String groupName) {
        DefenseGroup defenseGroup = new DefenseGroup();
        defenseGroup.setGroupname(groupName + UUID.randomUUID().toString().substring(0, 6));
        defenseGroup.setDefensetime(new Date(System.currentTimeMillis() + 86_400_000L));
        defenseGroup.setLocation("教学楼A101");
        defenseGroupService.save(defenseGroup);

        DefenseGroupStudent link = new DefenseGroupStudent();
        link.setGroupid(defenseGroup.getId());
        link.setStudentid(studentId);
        link.setSelectionid(selectionId);
        defenseGroupStudentMapper.insert(link);
    }

    private String saveBody(long studentId, String processScore,
                            String defenseScore, String teacherComment) {
        return """
                {
                  "studentId": "%d",
                  "processScore": %s,
                  "defenseScore": %s,
                  "teacherComment": %s
                }
                """.formatted(
                studentId,
                processScore,
                defenseScore,
                teacherComment == null
                        ? "null"
                        : "\"" + teacherComment + "\""
        );
    }

    private String queryBody(Long studentId, Long teacherId) {
        String studentIdJson = studentId == null ? "null" : "\"" + studentId + "\"";
        String teacherIdJson = teacherId == null ? "null" : "\"" + teacherId + "\"";
        return """
                {
                  "current": 1,
                  "pageSize": 10,
                  "studentId": %s,
                  "teacherId": %s
                }
                """.formatted(studentIdJson, teacherIdJson);
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
