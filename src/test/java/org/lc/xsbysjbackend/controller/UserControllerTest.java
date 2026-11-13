package org.lc.xsbysjbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.common.PasswordUtils;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.model.dto.user.UserLoginRequest;
import org.lc.xsbysjbackend.model.dto.user.UserRegisterRequest;
import org.lc.xsbysjbackend.model.entity.User;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginQueryCurrentUserAndLogout() throws Exception {
        String registerBody = """
                {
                  "useraccount": "student01",
                  "password": "123456",
                  "confirmPassword": "123456",
                  "userNo": "20260001",
                  "name": "测试学生"
                }
                """;

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("注册成功"));

        String loginBody = """
                {
                  "useraccount": "student01",
                  "password": "123456"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.useraccount").value("student01"))
                .andExpect(jsonPath("$.data.userNo").value("20260001"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn();

        HttpSession session = loginResult.getRequest().getSession(false);
        Cookie saTokenCookie = findCookie(loginResult.getResponse().getCookies(), "satoken");
        assertNotNull(session);
        assertNotNull(saTokenCookie);

        mockMvc.perform(get("/api/user/me")
                        .session((MockHttpSession) session)
                        .cookie(saTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("测试学生"));

        mockMvc.perform(post("/api/user/logout")
                        .session((MockHttpSession) session)
                        .cookie(saTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    //验证会话攻击
    @Test
    void loginRotatesSessionIdToPreventSessionFixation() throws Exception {
        String registerBody = """
                {
                  "useraccount": "sessionfix01",
                  "password": "123456",
                  "confirmPassword": "123456",
                  "userNo": "20261005",
                  "name": "会话固定测试用户"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        /*
         * 会话固定攻击示例：
         * 1. 攻击者先获得一个 Session ID，并诱导受害者的浏览器使用它；
         * 2. 受害者用这个 Session 登录；
         * 3. 如果服务端继续沿用原 Session ID，攻击者就能复用该 ID 冒充受害者。
         */
        MockHttpSession fixedSession = new MockHttpSession();
        String fixedSessionId = fixedSession.getId();
        fixedSession.setAttribute(UserConstant.USER_LOGIN_STATE, -1L);

        String loginBody = """
                {
                  "useraccount": "sessionfix01",
                  "password": "123456"
                }
                """;
        MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                        .session(fixedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.useraccount").value("sessionfix01"))
                .andReturn();

        HttpSession rotatedSession = loginResult.getRequest().getSession(false);
        User loginUser = userService.lambdaQuery()
                .eq(User::getUseraccount, "sessionfix01")
                .one();

        assertNotNull(rotatedSession);
        assertNotNull(loginUser);
        assertTrue(fixedSession.isInvalid());
        assertNotEquals(fixedSessionId, rotatedSession.getId());
        assertEquals(loginUser.getId(), rotatedSession.getAttribute(UserConstant.USER_LOGIN_STATE));
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

    @Test
    void registerRejectsAccountAndPasswordOutsideAllowedLength() throws Exception {
        String shortAccountBody = """
                {
                  "useraccount": "abc",
                  "password": "1234",
                  "confirmPassword": "1234",
                  "userNo": "20260002",
                  "name": "测试学生二"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shortAccountBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("用户账号长度必须在4到23之间"));

        String longPasswordBody = """
                {
                  "useraccount": "student02",
                  "password": "12345678901234",
                  "confirmPassword": "12345678901234",
                  "userNo": "20260003",
                  "name": "测试学生三"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(longPasswordBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("用户密码长度必须在4到13之间"));
    }

    @Test
    void registerSupportsStudentAndTeacherButRejectsAdmin() throws Exception {
        String teacherBody = """
                {
                  "useraccount": "registerteacher",
                  "password": "123456",
                  "confirmPassword": "123456",
                  "userNo": "T20261001",
                  "name": "注册教师",
                  "role": "teacher",
                  "department": "计算机学院"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String loginBody = """
                {
                  "useraccount": "registerteacher",
                  "password": "123456"
                }
                """;
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.role").value("teacher"))
                .andExpect(jsonPath("$.data.department").value("计算机学院"));

        String adminBody = """
                {
                  "useraccount": "registeradmin",
                  "password": "123456",
                  "confirmPassword": "123456",
                  "userNo": "A20261001",
                  "name": "注册管理员",
                  "role": "admin"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("公开注册用户类型只能为学生或教师"));
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUseraccount("secure01");
        registerRequest.setPassword("123456");
        registerRequest.setConfirmPassword("123456");
        registerRequest.setUserNo("20261001");
        registerRequest.setName("密码测试用户");

        long userId = userService.userRegister(registerRequest);
        User savedUser = userService.getById(userId);

        assertNotNull(savedUser);
        assertNotEquals("123456", savedUser.getPassword());
        assertTrue(PasswordUtils.isEncoded(savedUser.getPassword()));
        assertTrue(PasswordUtils.matches("123456", savedUser.getPassword()));
    }

    @Test
    void legacyPlaintextPasswordIsMigratedAfterLogin() {
        User legacyUser = new User();
        legacyUser.setUseraccount("legacy01");
        legacyUser.setUserNo("20261002");
        legacyUser.setPassword("654321");
        legacyUser.setName("历史用户");
        legacyUser.setRole(UserConstant.STUDENT_ROLE);
        userService.save(legacyUser);

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUseraccount("legacy01");
        loginRequest.setPassword("654321");
        userService.userLogin(loginRequest);

        User migratedUser = userService.getById(legacyUser.getId());
        assertNotNull(migratedUser);
        assertTrue(PasswordUtils.isEncoded(migratedUser.getPassword()));
        assertTrue(PasswordUtils.matches("654321", migratedUser.getPassword()));
    }

    @Test
    void normalUserCannotAccessAdminApi() throws Exception {
        Cookie token = registerStudentAndLogin("roleuser01", "20261003");
        String queryBody = """
                {
                  "current": 1,
                  "pageSize": 10
                }
                """;

        mockMvc.perform(post("/api/user/list/page/vo")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("无权限执行该操作"));
    }

    @Test
    void normalUserCannotPromoteRoleThroughUpdate() throws Exception {
        Cookie token = registerStudentAndLogin("roleuser02", "20261004");
        User user = userService.lambdaQuery()
                .eq(User::getUseraccount, "roleuser02")
                .one();
        assertNotNull(user);

        String updateBody = """
                {
                  "id": "%s",
                  "role": "admin"
                }
                """.formatted(user.getId());
        mockMvc.perform(post("/api/user/update")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("普通用户不能修改角色"));
    }

    @Test
    void studentCanReadTeacherOptions() throws Exception {
        UserAddRequest teacherRequest = new UserAddRequest();
        teacherRequest.setUseraccount("filterteacher");
        teacherRequest.setPassword("123456");
        teacherRequest.setUserNo("T20264001");
        teacherRequest.setName("筛选教师");
        teacherRequest.setRole(UserConstant.TEACHER_ROLE);
        teacherRequest.setDepartment("计算机学院");
        userService.addUser(teacherRequest);

        Cookie token = registerStudentAndLogin("filterstudent01", "S20264001");
        mockMvc.perform(get("/api/user/teachers")
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].userNo", hasItem("T20264001")))
                .andExpect(jsonPath("$.data[*].name", hasItem("筛选教师")))
                .andExpect(jsonPath("$.data[*].department", hasItem("计算机学院")));
    }

    @Test
    void adminCanManageUsers() throws Exception {
        UserAddRequest adminRequest = new UserAddRequest();
        adminRequest.setUseraccount("admin01");
        adminRequest.setPassword("123456");
        adminRequest.setUserNo("T20260001");
        adminRequest.setName("管理员");
        adminRequest.setRole(UserConstant.ADMIN_ROLE);
        userService.addUser(adminRequest);

        String loginBody = """
                {
                  "useraccount": "admin01",
                  "password": "123456"
                }
                """;
        MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        Cookie saTokenCookie = findCookie(loginResult.getResponse().getCookies(), "satoken");
        assertNotNull(session);
        assertNotNull(saTokenCookie);

        String addBody = """
                {
                  "useraccount": "managed01",
                  "password": "123456",
                  "userNo": "20260010",
                  "name": "被管理用户",
                  "role": "student",
                  "department": "计算机学院"
                }
                """;
        MvcResult addResult = mockMvc.perform(post("/api/user/add")
                        .session(session)
                        .cookie(saTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String managedUserId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").asText();

        mockMvc.perform(get("/api/user/get")
                        .param("id", managedUserId)
                        .session(session)
                        .cookie(saTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.useraccount").value("managed01"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        mockMvc.perform(get("/api/user/get/vo")
                        .param("id", managedUserId)
                        .session(session)
                        .cookie(saTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("被管理用户"));

        String updateBody = """
                {
                  "id": "%s",
                  "name": "已更新用户",
                  "role": "teacher",
                  "department": "软件学院",
                  "avatarUrl": "https://example.com/avatar.png"
                }
                """.formatted(managedUserId);
        mockMvc.perform(post("/api/user/update")
                        .session(session)
                        .cookie(saTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        String queryBody = """
                {
                  "current": 1,
                  "pageSize": 10,
                  "name": "已更新"
                }
                """;
        mockMvc.perform(post("/api/user/list/page/vo")
                        .session(session)
                        .cookie(saTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].role").value("teacher"));

        String deleteBody = """
                {
                  "id": "%s"
                }
                """.formatted(managedUserId);
        mockMvc.perform(post("/api/user/delete")
                        .session(session)
                        .cookie(saTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    private Cookie registerStudentAndLogin(String useraccount, String userNo) throws Exception {
        String registerBody = """
                {
                  "useraccount": "%s",
                  "password": "123456",
                  "confirmPassword": "123456",
                  "userNo": "%s",
                  "name": "角色测试用户"
                }
                """.formatted(useraccount, userNo);
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

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
}
