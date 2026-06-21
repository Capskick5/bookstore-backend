package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.ChangePasswordRequest;
import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.request.RefreshRequest;
import edu.fpt.sba301.bookstore.dto.request.RegisterRequest;
import edu.fpt.sba301.bookstore.dto.request.UpdateProfileRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.LoginResponse;
import edu.fpt.sba301.bookstore.dto.response.ProfileResponse;
import edu.fpt.sba301.bookstore.entity.RefreshToken;
import edu.fpt.sba301.bookstore.security.JwtTokenProvider;
import edu.fpt.sba301.bookstore.service.AuthService;
import edu.fpt.sba301.bookstore.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;

import static edu.fpt.sba301.bookstore.config.SwaggerConfig.BEARER_AUTH;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and profile")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse tokens = authService.login(request);
        ApiResponse<LoginResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(tokens);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register a new customer account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(201);
        response.setMessage("Created");
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Principal principal) {
        ProfileResponse profile = authService.getProfile(principal.getName());
        ApiResponse<ProfileResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(profile);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(Principal principal,
                                                                      @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse profile = authService.updateProfile(principal.getName(), request);
        ApiResponse<ProfileResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(profile);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Principal principal,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getName(), request);
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestBody RefreshRequest req) {
        ApiResponse<LoginResponse> response = new ApiResponse<>();

        RefreshToken stored = refreshTokenService.validateAndGet(req.refreshToken());
        RefreshToken newRefresh = refreshTokenService.rotate(stored);
        String newAccessToken = jwtTokenProvider.generateToken(stored.getUser().getEmail(), stored.getUser().getRole());

        LoginResponse data = new LoginResponse(newAccessToken, newRefresh.getToken());
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        try {
            RefreshToken stored = refreshTokenService.validateAndGet(req.refreshToken());
            refreshTokenService.revoke(stored);
        } catch (ResponseStatusException e) {
            // ignore or return 204 anyway to avoid leaking token validity
        }
        return ResponseEntity.noContent().build();
    }
}
