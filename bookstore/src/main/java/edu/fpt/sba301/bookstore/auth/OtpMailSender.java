package edu.fpt.sba301.bookstore.auth;

public interface OtpMailSender {
    void sendRegistrationOtp(String email, String otp, String displayName);
}
