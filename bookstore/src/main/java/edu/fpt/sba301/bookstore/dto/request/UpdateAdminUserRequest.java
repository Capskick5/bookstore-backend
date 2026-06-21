package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateAdminUserRequest(
        @Pattern(regexp = "ADMIN|CUSTOMER") String role,
        Boolean enabled
) {
}
