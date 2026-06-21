package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplyVoucherRequest(
        @NotBlank String code,
        @NotNull @Min(0) Long cartSubtotal
) {
}
