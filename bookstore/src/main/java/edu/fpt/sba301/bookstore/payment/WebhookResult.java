package edu.fpt.sba301.bookstore.payment;

public record WebhookResult(
        boolean success,
        Long orderId,
        Long amount,
        String transactionId,
        String message
) {
}
