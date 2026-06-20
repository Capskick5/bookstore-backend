package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.response.LoginResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.security.JwtTokenProvider;
import edu.fpt.sba301.bookstore.service.AuthService;
import edu.fpt.sba301.bookstore.entity.RefreshToken;
import edu.fpt.sba301.bookstore.service.RefreshTokenService;
import edu.fpt.sba301.bookstore.repository.RefreshTokenRepository;
import edu.fpt.sba301.bookstore.dto.request.RegisterRequest;
import edu.fpt.sba301.bookstore.dto.request.UpdateProfileRequest;
import edu.fpt.sba301.bookstore.dto.request.ChangePasswordRequest;
import edu.fpt.sba301.bookstore.dto.response.ProfileResponse;
import edu.fpt.sba301.bookstore.enums.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account disabled");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtTokenProvider.generateToken(user.getEmail(), user.getRole());
        RefreshToken dbRefreshToken = refreshTokenService.createTokenForUser(user);

        return new LoginResponse(accessToken, dbRefreshToken.getToken());
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String password = request.password();
        if (password == null || password.length() < 8 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long and contain at least one letter and one digit");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(request.fullName());
        user.setRole(Role.CUSTOMER.name());
        user.setEnabled(true);
        user.setPoints(0L);
        user.setLifetimePoints(0L);
        user.setTier("SILVER");
        user.setCreatedAt(OffsetDateTime.now());

        userRepository.save(user);
    }

    @Override
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setFullName(request.fullName());
        User saved = userRepository.save(user);
        return mapToProfileResponse(saved);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect current password");
        }

        String newPassword = request.newPassword();
        if (newPassword == null || newPassword.length() < 8 || !newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters long and contain at least one letter and one digit");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all user refresh tokens to force re-login
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);
        for (RefreshToken t : tokens) {
            t.setRevoked(true);
        }
        refreshTokenRepository.saveAll(tokens);
    }

    private ProfileResponse mapToProfileResponse(User user) {
        // dynamic tier calculation based on lifetime points
        long points = user.getLifetimePoints() != null ? user.getLifetimePoints() : 0L;
        String tier = "SILVER";
        if (points >= 5000) {
            tier = "PLATINUM";
        } else if (points >= 1000) {
            tier = "GOLD";
        }

        // update tier in DB if it changed
        if (!tier.equals(user.getTier())) {
            user.setTier(tier);
            userRepository.save(user);
        }

        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getPoints(),
                tier,
                user.getLifetimePoints()
        );
    }
}