package edu.fpt.sba301.bookstore;

import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.response.MediaUploadResponse;
import edu.fpt.sba301.bookstore.media.MediaUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MediaUploadTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaUploadService mediaUploadService;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest adminLogin = new LoginRequest("admin@example.com", "adminpassword123");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"adminpassword123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = extractToken(adminResult);

        LoginRequest customerLogin = new LoginRequest("test@example.com", "password123");
        MvcResult customerResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        customerToken = extractToken(customerResult);
    }

    @Test
    void coverUploadRequiresAdminRole() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/admin/media/cover").file(file)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void coverUploadReturnsUrlWhenServiceConfigured() throws Exception {
        when(mediaUploadService.uploadCover(any())).thenReturn(
                new MediaUploadResponse("https://res.cloudinary.com/demo/cover.jpg", "bookverse/covers/demo"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/admin/media/cover").file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://res.cloudinary.com/demo/cover.jpg"))
                .andExpect(jsonPath("$.data.publicId").value("bookverse/covers/demo"));
    }

    @Test
    void avatarUploadUpdatesProfile() throws Exception {
        when(mediaUploadService.uploadAvatar(any())).thenReturn(
                new MediaUploadResponse("https://res.cloudinary.com/demo/avatar.jpg", "bookverse/avatars/demo"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/auth/me/avatar").file(file)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value("https://res.cloudinary.com/demo/avatar.jpg"));
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        Map<?, ?> map = new tools.jackson.databind.json.JsonMapper().readValue(body, Map.class);
        Map<?, ?> dataMap = (Map<?, ?>) map.get("data");
        return (String) dataMap.get("accessToken");
    }
}
