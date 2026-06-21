package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 255) String recipient,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 255) String line,
        @NotBlank @Size(max = 100) String city,
        Boolean isDefault
) {
}
