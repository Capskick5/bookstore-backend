package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;

public record ReindexTaskResponse(
        String id,
        String source,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime lastIndexedAt,
        String message) {
}
