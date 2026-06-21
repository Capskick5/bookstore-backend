package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderItemResponse(
        Long bookId,
        String titleSnapshot,
        Long unitPrice,
        Integer quantity
) {
}
