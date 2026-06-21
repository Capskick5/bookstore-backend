package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record MessageResponse(
        Long id,
        String role,
        String content,
        List<SourceResponse> sources,
        OffsetDateTime createdAt
) {
}
