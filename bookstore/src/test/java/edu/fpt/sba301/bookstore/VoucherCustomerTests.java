package edu.fpt.sba301.bookstore;

import edu.fpt.sba301.bookstore.dto.request.ApplyVoucherRequest;
import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VoucherCustomerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void customerCanApplyValidVoucher() throws Exception {
        String token = login("test@example.com", "password123");

        mockMvc.perform(post("/api/vouchers/apply")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ApplyVoucherRequest("SAVE10K", 50000L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SAVE10K"))
                .andExpect(jsonPath("$.data.discount").isNumber())
                .andExpect(jsonPath("$.data.estimatedTotal").isNumber());
    }

    @Test
    void applyVoucherRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/vouchers/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ApplyVoucherRequest("SAVE10K", 50000L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCanListApplicableVouchers() throws Exception {
        String token = login("test@example.com", "password123");

        mockMvc.perform(get("/api/me/vouchers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new LoginRequest(email, password, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> response = jsonMapper.readValue(body, Map.class);
        return (String) ((Map<?, ?>) response.get("data")).get("accessToken");
    }
}
