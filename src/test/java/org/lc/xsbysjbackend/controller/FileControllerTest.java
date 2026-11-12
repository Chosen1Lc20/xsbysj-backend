package org.lc.xsbysjbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.manager.CosManager;
import org.lc.xsbysjbackend.model.dto.user.UserRegisterRequest;
import org.lc.xsbysjbackend.model.entity.FileRecord;
import org.lc.xsbysjbackend.service.FileRecordService;
import org.lc.xsbysjbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private FileRecordService fileRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CosManager cosManager;

    @Test
    void loggedInUserCanUploadDocument() throws Exception {
        Cookie token = registerAndLogin("fileuser01", "20260101");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "毕业设计说明.docx",
                "application/octet-stream",
                "test document".getBytes(StandardCharsets.UTF_8)
        );
        String expectedUrl = "https://example.com/public/file.docx";
        when(cosManager.uploadFileToCOS(any(), anyString(), anyString())).thenReturn(expectedUrl);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.fileType").value("attachment"))
                .andExpect(jsonPath("$.data.key", startsWith("attachment/")))
                .andExpect(jsonPath("$.data.url").value(expectedUrl))
                .andExpect(jsonPath("$.data.fileName").value("毕业设计说明.docx"))
                .andExpect(jsonPath("$.data.fileExtension").value("docx"))
                .andExpect(jsonPath("$.data.fileSize").value(file.getSize()))
                .andReturn();

        verify(cosManager).uploadFileToCOS(
                any(),
                anyString(),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );

        long fileId = Long.parseLong(objectMapper.readTree(
                uploadResult.getResponse().getContentAsString()
        ).path("data").path("id").asText());
        FileRecord fileRecord = fileRecordService.getById(fileId);
        assertNotNull(fileRecord);
        assertEquals("attachment", fileRecord.getFileType());
        assertEquals("docx", fileRecord.getFileExtension());
        assertEquals("毕业设计说明.docx", fileRecord.getFileName());
        assertEquals(expectedUrl, fileRecord.getFileUrl());
        assertEquals(file.getSize(), fileRecord.getFileSize());
        assertNotNull(fileRecord.getUploadUserId());
    }

    @Test
    void imageIsStoredInAvatarDirectory() throws Exception {
        Cookie token = registerAndLogin("fileuser03", "20260103");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.PNG",
                MediaType.IMAGE_PNG_VALUE,
                "image content".getBytes(StandardCharsets.UTF_8)
        );
        when(cosManager.uploadFileToCOS(any(), anyString(), anyString()))
                .thenReturn("https://example.com/avatar/image.png");

        MvcResult uploadResult = mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fileType").value("avatar"))
                .andExpect(jsonPath("$.data.key", startsWith("avatar/")))
                .andExpect(jsonPath("$.data.contentType").value(MediaType.IMAGE_PNG_VALUE))
                .andReturn();

        verify(cosManager).uploadFileToCOS(any(), anyString(), eq(MediaType.IMAGE_PNG_VALUE));

        long fileId = Long.parseLong(objectMapper.readTree(
                uploadResult.getResponse().getContentAsString()
        ).path("data").path("id").asText());
        FileRecord fileRecord = fileRecordService.getById(fileId);
        assertNotNull(fileRecord);
        assertEquals("avatar", fileRecord.getFileType());
        assertEquals("png", fileRecord.getFileExtension());
    }

    @Test
    void uploadRejectsUnsupportedFileType() throws Exception {
        Cookie token = registerAndLogin("fileuser02", "20260102");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "invalid".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(
                        "仅支持 jpg、jpeg、png、gif、webp、pdf、doc、docx 格式"
                ));

        verify(cosManager, never()).uploadFileToCOS(any(), anyString(), anyString());
    }

    @Test
    void ownerCanReplaceFileAndDeleteOldObject() throws Exception {
        Cookie token = registerAndLogin("fileuser04", "20260104");
        MockMultipartFile originalFile = new MockMultipartFile(
                "file",
                "初稿.docx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "original".getBytes(StandardCharsets.UTF_8)
        );
        String oldUrl = "https://example.com/attachment/original.docx";
        String newUrl = "https://example.com/attachment/replacement.docx";
        when(cosManager.uploadFileToCOS(any(), anyString(), anyString()))
                .thenReturn(oldUrl, newUrl);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/file/upload")
                        .file(originalFile)
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        var uploadData = objectMapper.readTree(
                uploadResult.getResponse().getContentAsString()
        ).path("data");
        long fileId = Long.parseLong(uploadData.path("id").asText());
        String oldObjectKey = uploadData.path("key").asText();

        MockMultipartFile replacementFile = new MockMultipartFile(
                "file",
                "终稿.docx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "replacement".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/file/update")
                        .file(replacementFile)
                        .param("id", Long.toString(fileId))
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(Long.toString(fileId)))
                .andExpect(jsonPath("$.data.fileName").value("终稿.docx"))
                .andExpect(jsonPath("$.data.url").value(newUrl))
                .andExpect(jsonPath("$.data.key", startsWith("attachment/")));

        verify(cosManager).deleteObject(oldObjectKey);
        FileRecord updatedRecord = fileRecordService.getById(fileId);
        assertNotNull(updatedRecord);
        assertEquals("终稿.docx", updatedRecord.getFileName());
        assertEquals(newUrl, updatedRecord.getFileUrl());
        assertEquals("attachment", updatedRecord.getFileType());
        assertEquals(replacementFile.getSize(), updatedRecord.getFileSize());
    }

    @Test
    void ownerCanRenameFileWithoutReplacingObject() throws Exception {
        Cookie token = registerAndLogin("fileuser05", "20260105");
        MockMultipartFile originalFile = new MockMultipartFile(
                "file",
                "论文初稿.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes(StandardCharsets.UTF_8)
        );
        String url = "https://example.com/attachment/paper.pdf";
        when(cosManager.uploadFileToCOS(any(), anyString(), anyString())).thenReturn(url);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/file/upload")
                        .file(originalFile)
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        var uploadData = objectMapper.readTree(
                uploadResult.getResponse().getContentAsString()
        ).path("data");
        long fileId = Long.parseLong(uploadData.path("id").asText());
        String objectKey = uploadData.path("key").asText();

        mockMvc.perform(multipart("/api/file/update")
                        .param("id", Long.toString(fileId))
                        .param("fileName", "毕业设计终稿.pdf")
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fileName").value("毕业设计终稿.pdf"))
                .andExpect(jsonPath("$.data.key").value(objectKey))
                .andExpect(jsonPath("$.data.url").value(url));

        verify(cosManager, never()).deleteObject(anyString());
        FileRecord updatedRecord = fileRecordService.getById(fileId);
        assertNotNull(updatedRecord);
        assertEquals("毕业设计终稿.pdf", updatedRecord.getFileName());
        assertEquals(objectKey, updatedRecord.getFileKey());
    }

    private Cookie registerAndLogin(String useraccount, String userNo) throws Exception {
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUseraccount(useraccount);
        registerRequest.setPassword("123456");
        registerRequest.setConfirmPassword("123456");
        registerRequest.setUserNo(userNo);
        registerRequest.setName("文件上传测试用户");
        userService.userRegister(registerRequest);

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
