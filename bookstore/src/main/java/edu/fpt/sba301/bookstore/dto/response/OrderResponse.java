package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        Long subtotal,
        Long discount,
        Long shippingFee,
        Long total,
        String voucherCode,
        Long pointsUsed,
        Long pointsEarned,
        String paymentUrl,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        List<OrderItemResponse> items
) {
}
