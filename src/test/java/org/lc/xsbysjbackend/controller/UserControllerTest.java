package org.lc.xsbysjbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.constant.UserConstant;
import org.lc.xsbysjbackend.model.dto.user.UserAddRequest;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                  "dept": "计算机学院"
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
                  "dept": "软件学院",
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
}
