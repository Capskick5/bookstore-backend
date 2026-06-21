package edu.fpt.sba301.bookstore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminStatsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private tools.jackson.databind.json.JsonMapper jsonMapper;

    @Test
    void adminCanFetchDashboardStats() throws Exception {
        String adminToken = login("admin@example.com", "adminpassword123");

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBooks").isNumber())
                .andExpect(jsonPath("$.data.totalCategories").isNumber())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalOrders").isNumber())
                .andExpect(jsonPath("$.data.totalRevenue").isNumber())
                .andExpect(jsonPath("$.data.topBooks").isArray());
    }

    @Test
    void customerCannotFetchDashboardStats() throws Exception {
        String customerToken = login("test@example.com", "password123");

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    private String login(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new edu.fpt.sba301.bookstore.dto.request.LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        var map = jsonMapper.readValue(result.getResponse().getContentAsString(), java.util.Map.class);
        return (String) ((java.util.Map<?, ?>) map.get("data")).get("accessToken");
    }
}
