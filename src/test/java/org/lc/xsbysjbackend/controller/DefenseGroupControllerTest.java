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
class DefenseGroupControllerTest {

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
    void adminCanCreateUpdateAndDeleteDefenseGroup() throws Exception {
        LoginUser admin = createAndLogin("defenseadmin01", "A20266001",
                UserConstant.ADMIN_ROLE, "答辩管理员");
        LoginUser teacherOne = createAndLogin("defenseteacher01", "T20266001",
                UserConstant.TEACHER_ROLE, "答辩教师一");
        LoginUser teacherTwo = createAndLogin("defenseteacher02", "T20266002",
                UserConstant.TEACHER_ROLE, "答辩教师二");
        LoginUser teacherThree = createAndLogin("defenseteacher03", "T20266003",
                UserConstant.TEACHER_ROLE, "答辩教师三");
        LoginUser studentOne = createAndLogin("defensestudent01", "S20266001",
                UserConstant.STUDENT_ROLE, "答辩学生一");
        LoginUser studentTwo = createAndLogin("defensestudent02", "S20266002",
                UserConstant.STUDENT_ROLE, "答辩学生二");
        createApprovedSelection(teacherOne.id(), studentOne.id(), "答辩测试课题一");
        createApprovedSelection(teacherTwo.id(), studentTwo.id(), "答辩测试课题二");

        String groupId = addDefenseGroup(
                admin.cookie(),
                "第一答辩组",
                "2099-12-01 09:00:00",
                "教学楼A101",
                "请提前二十分钟到场",
                new long[]{teacherOne.id(), teacherTwo.id()},
                new long[]{studentOne.id(), studentTwo.id()}
        );

        mockMvc.perform(get("/api/defense/group/get/vo")
                        .param("id", groupId)
                        .cookie(admin.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.groupName").value("第一答辩组"))
                .andExpect(jsonPath("$.data.defenseTime").value("2099-12-01 09:00:00"))
                .andExpect(jsonPath("$.data.location").value("教学楼A101"))
                .andExpect(jsonPath("$.data.remark").value("请提前二十分钟到场"))
                .andExpect(jsonPath("$.data.teachers.length()").value(2))
                .andExpect(jsonPath("$.data.students.length()").value(2))
                .andExpect(jsonPath("$.data.students[0].topicTitle").exists());

        mockMvc.perform(post("/api/defense/group/list/page/vo")
                        .cookie(teacherOne.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody("第一答辩组", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(post("/api/defense/group/list/page/vo")
                        .cookie(studentOne.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody(null, studentOne.id(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(groupId));

        mockMvc.perform(post("/api/defense/group/list/page/vo")
                        .cookie(teacherThree.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody("第一答辩组", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/api/defense/group/list/page/vo")
                        .cookie(admin.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody(null, null, teacherTwo.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(post("/api/defense/group/update")
                        .cookie(admin.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                groupId,
                                "调整后答辩组",
                                "2099-12-02 14:00:00",
                                "教学楼B202",
                                new long[]{teacherTwo.id(), teacherThree.id()},
                                new long[]{studentTwo.id()}
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/defense/group/get/vo")
                        .param("id", groupId)
                        .cookie(teacherOne.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权查看该答辩组"));

        mockMvc.perform(get("/api/defense/group/get/vo")
                        .param("id", groupId)
                        .cookie(teacherThree.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.groupName").value("调整后答辩组"))
                .andExpect(jsonPath("$.data.teachers.length()").value(2))
                .andExpect(jsonPath("$.data.students.length()").value(1));

        mockMvc.perform(get("/api/defense/group/get/vo")
                        .param("id", groupId)
                        .cookie(studentOne.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(get("/api/defense/group/get/vo")
                        .param("id", groupId)
                        .cookie(studentTwo.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.location").value("教学楼B202"));

        deleteDefenseGroup(admin.cookie(), groupId);

        mockMvc.perform(get("/api/defense/group/get/vo")
                        .param("id", groupId)
                        .cookie(admin.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("答辩组不存在"));
    }

    @Test
    void studentCannotBelongToMultipleGroupsAndOnlyAdminCanManage() throws Exception {
        LoginUser admin = createAndLogin("defenseadmin02", "A20266002",
                UserConstant.ADMIN_ROLE, "答辩管理员二");
        LoginUser teacher = createAndLogin("defenseteacher04", "T20266004",
                UserConstant.TEACHER_ROLE, "答辩教师四");
        LoginUser student = createAndLogin("defensestudent03", "S20266003",
                UserConstant.STUDENT_ROLE, "答辩学生三");
        LoginUser studentWithoutSelection = createAndLogin(
                "defensestudent04", "S20266004",
                UserConstant.STUDENT_ROLE, "无选题答辩学生"
        );
        createApprovedSelection(teacher.id(), student.id(), "答辩重复分配测试课题");

        String firstGroupId = addDefenseGroup(
                admin.cookie(),
                "第二答辩组",
                "2099-12-03 09:00:00",
                "教学楼C301",
                null,
                new long[]{teacher.id()},
                new long[]{student.id()}
        );

        mockMvc.perform(post("/api/defense/group/add")
                        .cookie(admin.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(
                                "第三答辩组",
                                "2099-12-04 09:00:00",
                                "教学楼C302",
                                null,
                                new long[]{teacher.id()},
                                new long[]{student.id()}
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("学生已分配到其他答辩组"));

        mockMvc.perform(post("/api/defense/group/add")
                        .cookie(admin.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(
                                "无选题学生答辩组",
                                "2099-12-04 10:00:00",
                                "教学楼C303",
                                null,
                                new long[]{teacher.id()},
                                new long[]{studentWithoutSelection.id()}
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message")
                        .value("存在尚未审核通过选题的学生，不能加入答辩组"));

        mockMvc.perform(post("/api/defense/group/add")
                        .cookie(teacher.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(
                                "教师创建答辩组",
                                "2099-12-05 09:00:00",
                                "教学楼D401",
                                null,
                                new long[]{teacher.id()},
                                new long[]{}
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权限执行该操作"));

        mockMvc.perform(post("/api/defense/group/add")
                        .cookie(admin.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(
                                "重复教师答辩组",
                                "2099-12-05 10:00:00",
                                "教学楼D402",
                                null,
                                new long[]{teacher.id(), teacher.id()},
                                new long[]{}
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("答辩教师不能重复"));

        deleteDefenseGroup(admin.cookie(), firstGroupId);
    }

    private long createApprovedSelection(long teacherId, long studentId, String topicTitle) {
        Topic topic = new Topic();
        topic.setTeacherid(teacherId);
        topic.setTitle(topicTitle);
        topic.setDescription("答辩管理测试课题");
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

    private String addDefenseGroup(Cookie cookie, String groupName, String defenseTime,
                                   String location, String remark, long[] teacherIds,
                                   long[] studentIds) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/defense/group/add")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(
                                groupName,
                                defenseTime,
                                location,
                                remark,
                                teacherIds,
                                studentIds
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readData(result).asText();
    }

    private String addBody(String groupName, String defenseTime, String location,
                           String remark, long[] teacherIds, long[] studentIds) {
        return """
                {
                  "groupName": "%s",
                  "defenseTime": "%s",
                  "location": "%s",
                  "remark": %s,
                  "teacherIds": %s,
                  "studentIds": %s
                }
                """.formatted(
                groupName,
                defenseTime,
                location,
                remark == null ? "null" : "\"" + remark + "\"",
                idArray(teacherIds),
                idArray(studentIds)
        );
    }

    private String updateBody(String groupId, String groupName, String defenseTime,
                              String location, long[] teacherIds, long[] studentIds) {
        return """
                {
                  "id": "%s",
                  "groupName": "%s",
                  "defenseTime": "%s",
                  "location": "%s",
                  "teacherIds": %s,
                  "studentIds": %s
                }
                """.formatted(
                groupId,
                groupName,
                defenseTime,
                location,
                idArray(teacherIds),
                idArray(studentIds)
        );
    }

    private String idArray(long[] ids) {
        return java.util.Arrays.stream(ids)
                .mapToObj(id -> "\"" + id + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String queryBody(String groupName, Long studentId, Long teacherId) {
        String groupNameJson = groupName == null ? "null" : "\"" + groupName + "\"";
        String studentIdJson = studentId == null ? "null" : "\"" + studentId + "\"";
        String teacherIdJson = teacherId == null ? "null" : "\"" + teacherId + "\"";
        return """
                {
                  "current": 1,
                  "pageSize": 10,
                  "groupName": %s,
                  "studentId": %s,
                  "teacherId": %s
                }
                """.formatted(groupNameJson, studentIdJson, teacherIdJson);
    }

    private void deleteDefenseGroup(Cookie cookie, String groupId) throws Exception {
        mockMvc.perform(post("/api/defense/group/delete")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s"
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
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
