package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.request.ChangePasswordRequest;
import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.request.RegisterRequest;
import edu.fpt.sba301.bookstore.dto.request.UpdateProfileRequest;
import edu.fpt.sba301.bookstore.dto.request.VerifyRegistrationRequest;
import edu.fpt.sba301.bookstore.dto.response.LoginResponse;
import edu.fpt.sba301.bookstore.dto.response.ProfileResponse;
import edu.fpt.sba301.bookstore.auth.OtpMailSender;
import edu.fpt.sba301.bookstore.entity.RefreshToken;
import edu.fpt.sba301.bookstore.entity.RegistrationOtp;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.enums.Role;
import edu.fpt.sba301.bookstore.repository.RefreshTokenRepository;
import edu.fpt.sba301.bookstore.repository.RegistrationOtpRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.security.JwtTokenProvider;
import edu.fpt.sba301.bookstore.service.AuthService;
import edu.fpt.sba301.bookstore.service.CartService;
import edu.fpt.sba301.bookstore.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RegistrationOtpRepository registrationOtpRepository;
    private final OtpMailSender otpMailSender;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CartService cartService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account disabled");
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email not verified");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        cartService.mergeGuestCart(user, request.guestCartItems());

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

        validatePasswordPolicy(request.password());

        String otp = generateOtp();
        registrationOtpRepository.deleteByEmail(request.email());

        RegistrationOtp pending = new RegistrationOtp();
        pending.setEmail(request.email());
        pending.setFullName(request.fullName());
        pending.setPasswordHash(passwordEncoder.encode(request.password()));
        pending.setOtpHash(passwordEncoder.encode(otp));
        pending.setExpiresAt(OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        pending.setAttempts(0);
        pending.setCreatedAt(OffsetDateTime.now());
        registrationOtpRepository.save(pending);

        otpMailSender.sendRegistrationOtp(request.email(), otp, request.fullName());
    }

    @Override
    @Transactional
    public void verifyRegistration(VerifyRegistrationRequest request) {
        RegistrationOtp pending = registrationOtpRepository.findFirstByEmailOrderByCreatedAtDesc(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No pending registration found for this email"));

        if (pending.getExpiresAt().isBefore(OffsetDateTime.now())) {
            registrationOtpRepository.deleteByEmail(request.email());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code expired");
        }

        if (pending.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Too many invalid attempts. Please register again.");
        }

        if (!passwordEncoder.matches(request.otp(), pending.getOtpHash())) {
            pending.setAttempts(pending.getAttempts() + 1);
            registrationOtpRepository.save(pending);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }

        if (userRepository.existsByEmail(request.email())) {
            registrationOtpRepository.deleteByEmail(request.email());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setEmail(pending.getEmail());
        user.setPasswordHash(pending.getPasswordHash());
        user.setFullName(pending.getFullName());
        user.setRole(Role.CUSTOMER.name());
        user.setAuthProvider("LOCAL");
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setPoints(0L);
        user.setLifetimePoints(0L);
        user.setTier("SILVER");
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);
        registrationOtpRepository.deleteByEmail(request.email());
    }

    @Override
    @Transactional
    public void resendRegistrationOtp(String email) {
        RegistrationOtp pending = registrationOtpRepository.findFirstByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No pending registration found for this email"));

        String otp = generateOtp();
        pending.setOtpHash(passwordEncoder.encode(otp));
        pending.setExpiresAt(OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        pending.setAttempts(0);
        registrationOtpRepository.save(pending);

        otpMailSender.sendRegistrationOtp(email, otp, pending.getFullName());
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters long and contain at least one letter and one digit");
        }
    }

    private String generateOtp() {
        return String.valueOf(SECURE_RANDOM.nextInt(900_000) + 100_000);
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
    public ProfileResponse updateAvatar(String email, String avatarUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setAvatarUrl(avatarUrl);
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

        validatePasswordPolicy(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(tokens);
    }

    private ProfileResponse mapToProfileResponse(User user) {
        long points = user.getLifetimePoints() != null ? user.getLifetimePoints() : 0L;
        String tier = "SILVER";
        if (points >= 5000) {
            tier = "PLATINUM";
        } else if (points >= 1000) {
            tier = "GOLD";
        }

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
                user.getLifetimePoints(),
                user.getAvatarUrl()
        );
    }
}
