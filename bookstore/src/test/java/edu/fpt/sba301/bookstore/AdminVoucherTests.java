package edu.fpt.sba301.bookstore;

import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.request.VoucherRequest;
import edu.fpt.sba301.bookstore.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminVoucherTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private VoucherRepository voucherRepository;

    @Test
    void adminCreatesAndListsVoucher() throws Exception {
        String token = login("admin@example.com", "adminpassword123");
        String code = "MVP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        VoucherRequest request = new VoucherRequest(
                code, "FIXED", 10000L, 50000L, null, 100, 1,
                OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now().plusDays(30), true);

        mockMvc.perform(post("/api/admin/vouchers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(code));

        mockMvc.perform(get("/api/admin/vouchers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '%s')]".formatted(code)).exists());

        voucherRepository.findByCodeIgnoreCase(code).ifPresent(voucherRepository::delete);
    }

    @Test
    void customerCannotCreateVoucher() throws Exception {
        String token = login("test@example.com", "password123");
        VoucherRequest request = new VoucherRequest(
                "DENIED", "FIXED", 10000L, 0L, null, 1, 1,
                null, null, true);

        mockMvc.perform(post("/api/admin/vouchers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
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
