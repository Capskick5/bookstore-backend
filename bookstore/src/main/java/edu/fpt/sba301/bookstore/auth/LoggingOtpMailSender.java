package edu.fpt.sba301.bookstore.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class LoggingOtpMailSender implements OtpMailSender {

    @Override
    public void sendRegistrationOtp(String email, String otp, String displayName) {
        log.info("BookVerse registration OTP for {} ({}): {}", email, displayName, otp);
    }
}
