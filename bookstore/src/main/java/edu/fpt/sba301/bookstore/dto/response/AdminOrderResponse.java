package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminOrderResponse(
        Long id,
        String status,
        String paymentMethod,
        Long subtotal,
        Long discount,
        Long shippingFee,
        Long total,
        String voucherCode,
        Long pointsUsed,
        Long pointsEarned,
        String customerEmail,
        String customerFullName,
        String addressSnapshot,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        Boolean manualRefundRequired,
        List<OrderItemResponse> items
) {
}
