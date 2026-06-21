package edu.fpt.sba301.bookstore.dto.response;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        Long subtotal
) {
}
