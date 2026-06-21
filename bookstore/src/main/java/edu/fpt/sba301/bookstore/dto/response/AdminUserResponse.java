package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        String role,
        Boolean enabled,
        OffsetDateTime createdAt
) {
}
