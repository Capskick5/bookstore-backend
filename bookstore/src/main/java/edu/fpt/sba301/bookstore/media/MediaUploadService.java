package edu.fpt.sba301.bookstore.media;

import edu.fpt.sba301.bookstore.dto.response.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaUploadService {

    boolean isConfigured();

    MediaUploadResponse uploadCover(MultipartFile file);

    MediaUploadResponse uploadAvatar(MultipartFile file);
}
