package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressRequest(
    @NotBlank(message = "Recipient name is required")
    String recipient,

    @NotBlank(message = "Phone number is required")
    String phone,

    @NotBlank(message = "Address line is required")
    String line,

    @NotBlank(message = "City is required")
    String city,

    @NotNull(message = "isDefault status is required")
    Boolean isDefault
) {}
