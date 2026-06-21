package edu.fpt.sba301.bookstore.dto.response;

import java.util.List;

public record CartItemResponse(
        Long bookId,
        String title,
        String author,
        String coverUrl,
        Long unitPrice,
        Integer quantity,
        Long lineTotal,
        Boolean active
) {
}
