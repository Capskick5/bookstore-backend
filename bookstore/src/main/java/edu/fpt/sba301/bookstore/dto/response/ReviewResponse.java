package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;

public record ReviewResponse(
        Long id,
        Long bookId,
        String reviewerName,
        Integer rating,
        String comment,
        OffsetDateTime createdAt
) {
}
