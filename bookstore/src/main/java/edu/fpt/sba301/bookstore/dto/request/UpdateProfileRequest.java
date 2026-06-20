package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank(message = "Full name is required")
    String fullName
) {}
