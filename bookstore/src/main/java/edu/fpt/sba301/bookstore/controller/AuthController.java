package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.LoginResponse;
import edu.fpt.sba301.bookstore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse tokens = authService.login(request.email(), request.password());
        ApiResponse<LoginResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(tokens);
        return ResponseEntity.ok(response);
    }
}
