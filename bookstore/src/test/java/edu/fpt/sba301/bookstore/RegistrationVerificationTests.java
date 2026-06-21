package edu.fpt.sba301.bookstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fpt.sba301.bookstore.auth.OtpMailSender;
import edu.fpt.sba301.bookstore.dto.request.RegisterRequest;
import edu.fpt.sba301.bookstore.dto.request.ResendRegistrationOtpRequest;
import edu.fpt.sba301.bookstore.dto.request.VerifyRegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationVerificationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OtpMailSender otpMailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AtomicReference<String> lastOtp;

    @BeforeEach
    void setUp() {
        lastOtp = AuthTestSupport.captureOtp(otpMailSender);
    }

    @Test
    void registerRequiresOtpVerificationBeforeLogin() throws Exception {
        String email = "verify-" + java.util.UUID.randomUUID() + "@example.com";
        String password = "securePass123";
        RegisterRequest registerRequest = new RegisterRequest(email, password, "Verify User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent to email"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new edu.fpt.sba301.bookstore.dto.request.LoginRequest(email, password))))
                .andExpect(status().isUnauthorized());

        AuthTestSupport.registerAndVerify(
                mockMvc,
                lastOtp,
                registerRequest,
                objectMapper.writeValueAsString(registerRequest));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new edu.fpt.sba301.bookstore.dto.request.LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void invalidOtpIsRejected() throws Exception {
        String email = "invalid-otp-" + java.util.UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest(email, "securePass123", "Invalid OTP User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        VerifyRegistrationRequest verifyRequest = new VerifyRegistrationRequest(email, "000000");
        mockMvc.perform(post("/api/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendOtpAllowsVerificationWithNewCode() throws Exception {
        String email = "resend-" + java.util.UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest(email, "securePass123", "Resend User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        String firstOtp = lastOtp.get();

        mockMvc.perform(post("/api/auth/register/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendRegistrationOtpRequest(email))))
                .andExpect(status().isOk());

        VerifyRegistrationRequest oldCode = new VerifyRegistrationRequest(email, firstOtp);
        mockMvc.perform(post("/api/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldCode)))
                .andExpect(status().isBadRequest());

        VerifyRegistrationRequest newCode = new VerifyRegistrationRequest(email, lastOtp.get());
        mockMvc.perform(post("/api/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCode)))
                .andExpect(status().isCreated());
    }
}
