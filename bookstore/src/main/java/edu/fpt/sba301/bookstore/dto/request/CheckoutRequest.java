package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CheckoutRequest(
        @NotNull Long addressId,
        @NotBlank String paymentMethod,
        String voucherCode,
        @PositiveOrZero Long pointsToRedeem
) {
}
