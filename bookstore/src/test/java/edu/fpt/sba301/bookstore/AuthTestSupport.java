package edu.fpt.sba301.bookstore;

import edu.fpt.sba301.bookstore.auth.OtpMailSender;
import edu.fpt.sba301.bookstore.dto.request.RegisterRequest;
import edu.fpt.sba301.bookstore.dto.request.VerifyRegistrationRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public static AtomicReference<String> captureOtp(OtpMailSender otpMailSender) {
        AtomicReference<String> lastOtp = new AtomicReference<>();
        doAnswer(invocation -> {
            lastOtp.set(invocation.getArgument(1));
            return null;
        }).when(otpMailSender).sendRegistrationOtp(anyString(), anyString(), anyString());
        return lastOtp;
    }

    public static void registerAndVerify(
            MockMvc mockMvc,
            AtomicReference<String> lastOtp,
            RegisterRequest registerRequest,
            String registerJson) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","otp":"%s"}
                                """.formatted(registerRequest.email(), lastOtp.get())))
                .andExpect(status().isCreated());
    }
}
