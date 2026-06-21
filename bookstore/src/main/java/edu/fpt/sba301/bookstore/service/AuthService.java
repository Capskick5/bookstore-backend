package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.ChangePasswordRequest;
import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.request.RegisterRequest;
import edu.fpt.sba301.bookstore.dto.request.UpdateProfileRequest;
import edu.fpt.sba301.bookstore.dto.request.VerifyRegistrationRequest;
import edu.fpt.sba301.bookstore.dto.response.LoginResponse;
import edu.fpt.sba301.bookstore.dto.response.ProfileResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);

    void verifyRegistration(VerifyRegistrationRequest request);

    void resendRegistrationOtp(String email);

    ProfileResponse getProfile(String email);

    ProfileResponse updateProfile(String email, UpdateProfileRequest request);

    ProfileResponse updateAvatar(String email, String avatarUrl);

    void changePassword(String email, ChangePasswordRequest request);
}
