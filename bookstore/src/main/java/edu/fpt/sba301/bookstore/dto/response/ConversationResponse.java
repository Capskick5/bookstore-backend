package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;

public record ConversationResponse(
        Long id,
        String title,
        OffsetDateTime createdAt
) {
}
