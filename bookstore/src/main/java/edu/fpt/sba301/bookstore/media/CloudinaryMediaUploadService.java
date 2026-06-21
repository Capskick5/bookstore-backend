package edu.fpt.sba301.bookstore.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import edu.fpt.sba301.bookstore.dto.response.MediaUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class CloudinaryMediaUploadService implements MediaUploadService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final Cloudinary cloudinary;
    private final boolean configured;

    public CloudinaryMediaUploadService(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret) {
        this.configured = isPresent(cloudName) && isPresent(apiKey) && isPresent(apiSecret);
        if (configured) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true));
        } else {
            this.cloudinary = null;
            log.warn("Cloudinary is not configured; media upload endpoints will return 503");
        }
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public MediaUploadResponse uploadCover(MultipartFile file) {
        return upload(file, "bookverse/covers", "c_limit,h_1200,w_800");
    }

    @Override
    public MediaUploadResponse uploadAvatar(MultipartFile file) {
        return upload(file, "bookverse/avatars", "c_fill,g_face,h_400,w_400");
    }

    private MediaUploadResponse upload(MultipartFile file, String folder, String transformation) {
        ensureConfigured();
        validateFile(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "transformation", transformation));
            return new MediaUploadResponse(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"));
        } catch (IOException ex) {
            log.warn("Failed to read upload file: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload file");
        } catch (Exception ex) {
            log.warn("Cloudinary upload failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Media upload failed");
        }
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Media upload is not configured. Set Cloudinary environment variables or paste an image URL.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be 5MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, or WebP images are allowed");
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
