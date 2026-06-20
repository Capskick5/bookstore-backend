package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GuestCartItemRequest(
        @NotNull Long bookId,
        @NotNull @Min(1) Integer quantity
) {
}
