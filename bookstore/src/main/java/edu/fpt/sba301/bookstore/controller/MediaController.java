package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.MediaUploadResponse;
import edu.fpt.sba301.bookstore.dto.response.ProfileResponse;
import edu.fpt.sba301.bookstore.media.MediaUploadService;
import edu.fpt.sba301.bookstore.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

import static edu.fpt.sba301.bookstore.config.SwaggerConfig.BEARER_AUTH;

@RestController
@RequiredArgsConstructor
@Tag(name = "Media", description = "Image upload via Cloudinary")
public class MediaController {

    private final MediaUploadService mediaUploadService;
    private final AuthService authService;

    @Operation(summary = "Upload a book cover image")
    @PostMapping("/api/admin/media/cover")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = BEARER_AUTH)
    public ResponseEntity<ApiResponse<MediaUploadResponse>> uploadCover(@RequestPart("file") MultipartFile file) {
        MediaUploadResponse data = mediaUploadService.uploadCover(file);
        ApiResponse<MediaUploadResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Upload the current user's avatar")
    @PostMapping("/api/auth/me/avatar")
    @SecurityRequirement(name = BEARER_AUTH)
    public ResponseEntity<ApiResponse<ProfileResponse>> uploadAvatar(
            Principal principal,
            @RequestPart("file") MultipartFile file) {
        MediaUploadResponse upload = mediaUploadService.uploadAvatar(file);
        ProfileResponse profile = authService.updateAvatar(principal.getName(), upload.url());
        ApiResponse<ProfileResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(profile);
        return ResponseEntity.ok(response);
    }
}
