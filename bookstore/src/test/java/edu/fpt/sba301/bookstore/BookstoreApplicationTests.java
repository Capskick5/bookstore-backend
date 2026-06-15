package edu.fpt.sba301.bookstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fpt.sba301.bookstore.dto.request.ChangePasswordRequest;
import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookstoreApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void contextLoads() {
    }

    @Test
    void testRegisterAndLoginAndProfileFlow() throws Exception {
        String email = "newuser-" + java.util.UUID.randomUUID() + "@example.com";
        String password = "securePass123";
        String fullName = "New User";

        // 1. Register a new user
        RegisterRequest registerReq = new RegisterRequest(email, password, fullName);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201));

        // 2. Login with registered user
        LoginRequest loginReq = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(responseBody, Map.class);
        Map<?, ?> dataMap = (Map<?, ?>) map.get("data");
        String accessToken = (String) dataMap.get("accessToken");

        // 3. Get profile
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.fullName").value(fullName))
                .andExpect(jsonPath("$.data.tier").value("SILVER"));

        // 4. Change password
        ChangePasswordRequest changePasswordReq = new ChangePasswordRequest(password, "newSecurePass456");
        mockMvc.perform(put("/api/auth/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 5. Try to login with old password -> should fail (401)
        LoginRequest oldLoginReq = new LoginRequest(email, password);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginReq)))
                .andExpect(status().isUnauthorized());

        // 6. Login with new password -> should succeed
        LoginRequest newLoginReq = new LoginRequest(email, "newSecurePass456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void testAddressCRUD() throws Exception {
        String email = "addruser-" + java.util.UUID.randomUUID() + "@example.com";
        String password = "securePass123";

        // Register
        RegisterRequest registerReq = new RegisterRequest(email, password, "Address User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest loginReq = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(responseBody, Map.class);
        Map<?, ?> dataMap = (Map<?, ?>) map.get("data");
        String accessToken = (String) dataMap.get("accessToken");

        // 1. Create address
        edu.fpt.sba301.bookstore.dto.request.AddressRequest addressReq =
                new edu.fpt.sba301.bookstore.dto.request.AddressRequest(
                        "Recipient Name", "0987654321", "123 Main St", "Hanoi", true
                );

        MvcResult createResult = mockMvc.perform(post("/api/auth/me/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recipient").value("Recipient Name"))
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        Map<?, ?> createMap = objectMapper.readValue(createBody, Map.class);
        Map<?, ?> addrData = (Map<?, ?>) createMap.get("data");
        Number addressId = (Number) addrData.get("id");

        // 2. Get addresses list
        mockMvc.perform(get("/api/auth/me/addresses")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(addressId.intValue()))
                .andExpect(jsonPath("$.data[0].recipient").value("Recipient Name"));

        // 3. Update address
        edu.fpt.sba301.bookstore.dto.request.AddressRequest updateReq =
                new edu.fpt.sba301.bookstore.dto.request.AddressRequest(
                        "Updated Recipient", "0987654321", "456 Side St", "HCM", false
                );

        mockMvc.perform(put("/api/auth/me/addresses/" + addressId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipient").value("Updated Recipient"))
                .andExpect(jsonPath("$.data.isDefault").value(false));

        // 4. Delete address
        mockMvc.perform(delete("/api/auth/me/addresses/" + addressId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 5. Verify list is empty
        mockMvc.perform(get("/api/auth/me/addresses")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
