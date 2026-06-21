package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateVoucherActiveRequest(
        @NotNull Boolean active
) {
}
