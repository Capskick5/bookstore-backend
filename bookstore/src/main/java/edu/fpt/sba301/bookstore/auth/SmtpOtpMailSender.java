package edu.fpt.sba301.bookstore.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SmtpOtpMailSender implements OtpMailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendRegistrationOtp(String email, String otp, String displayName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("BookVerse — Mã xác minh đăng ký");
        message.setText("""
                Xin chào %s,

                Mã xác minh đăng ký BookVerse của bạn là: %s

                Mã có hiệu lực trong 10 phút. Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.

                Trân trọng,
                BookVerse
                """.formatted(displayName, otp));
        mailSender.send(message);
    }
}
