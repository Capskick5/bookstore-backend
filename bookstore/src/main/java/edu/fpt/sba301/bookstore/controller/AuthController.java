package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.request.RefreshRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.LoginResponse;
import edu.fpt.sba301.bookstore.entity.RefreshToken;
import edu.fpt.sba301.bookstore.security.JwtTokenProvider;
import edu.fpt.sba301.bookstore.service.AuthService;
import edu.fpt.sba301.bookstore.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse tokens = authService.login(request);
        ApiResponse<LoginResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(tokens);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestBody RefreshRequest req) {
        ApiResponse<LoginResponse> response = new ApiResponse<>();

        // validate stored token
        RefreshToken stored = refreshTokenService.validateAndGet(req.refreshToken());

        // rotate
        RefreshToken newRefresh = refreshTokenService.rotate(stored);

        // create new access token (subject is user email)
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
