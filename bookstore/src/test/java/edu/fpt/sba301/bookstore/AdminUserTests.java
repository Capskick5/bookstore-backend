package edu.fpt.sba301.bookstore;

import edu.fpt.sba301.bookstore.dto.request.UpdateAdminUserRequest;
import edu.fpt.sba301.bookstore.repository.RefreshTokenRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private tools.jackson.databind.json.JsonMapper jsonMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void adminCanListUsers() throws Exception {
        String adminToken = login("admin@example.com", "adminpassword123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void adminCanDisableUserAndRevokeRefreshTokens() throws Exception {
        String adminToken = login("admin@example.com", "adminpassword123");
        var customer = userRepository.findByEmail("test@example.com").orElseThrow();
        customer.setEnabled(true);
        userRepository.save(customer);

        String customerToken = login("test@example.com", "password123");
        assertTrue(refreshTokenRepository.findAllByUser(customer).stream().anyMatch(t -> !t.getRevoked()));

        mockMvc.perform(patch("/api/admin/users/" + customer.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new UpdateAdminUserRequest(null, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        assertTrue(refreshTokenRepository.findAllByUser(customer).stream().allMatch(t -> t.getRevoked()));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        customer.setEnabled(true);
        userRepository.save(customer);
    }

    @Test
    void adminCannotModifySelf() throws Exception {
        String adminToken = login("admin@example.com", "adminpassword123");
        var admin = userRepository.findByEmail("admin@example.com").orElseThrow();

        mockMvc.perform(patch("/api/admin/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new UpdateAdminUserRequest("CUSTOMER", false))))
                .andExpect(status().isConflict());
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
