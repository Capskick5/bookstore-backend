package edu.fpt.sba301.bookstore.payment;

import java.time.OffsetDateTime;

public record WebhookResult(
        boolean success,
        Long orderId,
        Long amount,
        String transactionId,
        String message,
        OffsetDateTime authorizedAt
) {
}
