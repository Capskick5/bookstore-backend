package edu.fpt.sba301.bookstore.payment;

import edu.fpt.sba301.bookstore.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class MockPaymentService implements PaymentService {

    public static final String DEFAULT_SIGNING_SECRET = "mock-secret";

    @Value("${app.payment.mock.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.payment.mock.signing-secret:mock-secret}")
    private String signingSecret;

    @Override
    public PaymentResponse createPaymentUrl(Order order, String returnUrl) {
        String transactionId = "MOCK-" + UUID.randomUUID();
        String signature = sign(order.getId(), order.getTotal(), transactionId, signingSecret);
        String paymentUrl = baseUrl + "/api/payment/webhook/mock"
                + "?orderId=" + order.getId()
                + "&amount=" + order.getTotal()
                + "&transactionId=" + transactionId
                + "&signature=" + signature
                + "&status=success";
        return new PaymentResponse(paymentUrl, transactionId);
    }

    @Override
    public WebhookResult verifyWebhook(Map<String, String> params, Object requestBody) {
        Long orderId = parseLong(params.get("orderId"));
        Long amount = parseLong(params.get("amount"));
        String transactionId = params.get("transactionId");
        String signature = params.get("signature");
        String status = params.getOrDefault("status", "success");

        if (orderId == null || amount == null || transactionId == null || signature == null) {
            return new WebhookResult(false, orderId, amount, transactionId, "Missing webhook parameters", null);
        }
        String expected = sign(orderId, amount, transactionId, signingSecret);
        if (!expected.equals(signature)) {
            return new WebhookResult(false, orderId, amount, transactionId, "Invalid signature", null);
        }
        boolean success = "success".equalsIgnoreCase(status);
        OffsetDateTime authorizedAt = null;
        if (success) {
            String authorizedAtParam = params.get("authorizedAt");
            authorizedAt = authorizedAtParam != null && !authorizedAtParam.isBlank()
                    ? OffsetDateTime.parse(authorizedAtParam)
                    : OffsetDateTime.now();
        }
        return new WebhookResult(success, orderId, amount, transactionId, success ? "OK" : "Payment failed", authorizedAt);
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    public static String sign(Long orderId, Long amount, String transactionId) {
        return sign(orderId, amount, transactionId, DEFAULT_SIGNING_SECRET);
    }

    public static String sign(Long orderId, Long amount, String transactionId, String signingSecret) {
        try {
            String payload = orderId + "|" + amount + "|" + transactionId + "|" + signingSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign mock payment", e);
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
