package edu.fpt.sba301.bookstore.payment;

import edu.fpt.sba301.bookstore.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class PayOsPaymentService implements PaymentService {

    private static final DateTimeFormatter PAYOS_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAYOS_DESCRIPTION_MAX_LENGTH = 25;
    private static final String PAYOS_SUCCESS_CODE = "00";
    private static final String PAYOS_ITEM_NAME = "BookVerse order";

    private final ObjectProvider<PayOS> payOSProvider;

    @Value("${app.payment.payos.cancel-url:http://localhost:3000/payment/cancel}")
    private String cancelUrl;

    public PayOsPaymentService(ObjectProvider<PayOS> payOSProvider) {
        this.payOSProvider = payOSProvider;
    }

    @Override
    public PaymentResponse createPaymentUrl(Order order, String returnUrl) {
        PayOS payOS = requirePayOs();
        if (order.getExpiresAt() == null) {
            throw new IllegalStateException("Order expiresAt is required for PayOS payment link");
        }
        if (order.getId() == null) {
            throw new IllegalStateException("Order id is required before creating PayOS payment link");
        }
        if (order.getTotal() > Integer.MAX_VALUE) {
            throw new IllegalStateException("PayOS amount exceeds supported range");
        }

        try {
            long orderCode = order.getId();
            long amount = order.getTotal();
            String description = truncate("BookVerse order #" + order.getId(), PAYOS_DESCRIPTION_MAX_LENGTH);
            long expiredAt = order.getExpiresAt().toEpochSecond();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .expiredAt(expiredAt)
                    .item(PaymentLinkItem.builder()
                            .name(PAYOS_ITEM_NAME)
                            .quantity(1)
                            .price(amount)
                            .build())
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            String paymentLinkId = response.getPaymentLinkId() != null
                    ? response.getPaymentLinkId()
                    : "PAYOS-" + orderCode;
            return new PaymentResponse(response.getCheckoutUrl(), paymentLinkId);
        } catch (Exception e) {
            log.error("PayOS createPaymentUrl failed for order {}", order.getId(), e);
            throw new IllegalStateException("Unable to create PayOS payment link: " + e.getMessage(), e);
        }
    }

    @Override
    public WebhookResult verifyWebhook(Map<String, String> params, Object requestBody) {
        if (requestBody == null) {
            return new WebhookResult(false, null, null, null, "PayOS webhook body is required", null);
        }
        PayOS payOS = requirePayOs();
        try {
            WebhookData data = payOS.webhooks().verify(requestBody);
            Long orderId = data.getOrderCode();
            Long amount = data.getAmount() != null ? data.getAmount().longValue() : null;
            String transactionId = data.getPaymentLinkId() != null ? data.getPaymentLinkId() : data.getReference();
            boolean success = isSuccessfulPayment(data);
            OffsetDateTime authorizedAt = parseTransactionDateTime(data.getTransactionDateTime());
            String message = success ? "OK" : (data.getDesc() != null ? data.getDesc() : "Payment failed");
            return new WebhookResult(success, orderId, amount, transactionId, message, authorizedAt);
        } catch (Exception e) {
            log.warn("PayOS webhook verification failed: {}", e.getMessage());
            return new WebhookResult(false, null, null, null, e.getMessage(), null);
        }
    }

    @Override
    public String getProviderName() {
        return "payos";
    }

    private PayOS requirePayOs() {
        PayOS payOS = payOSProvider.getIfAvailable();
        if (payOS == null) {
            throw new IllegalStateException("PayOS is not configured. Set PAYOS_CLIENT_ID, PAYOS_API_KEY, and PAYOS_CHECKSUM_KEY.");
        }
        return payOS;
    }

    private boolean isSuccessfulPayment(WebhookData data) {
        if (data.getCode() != null && !PAYOS_SUCCESS_CODE.equals(data.getCode())) {
            return false;
        }
        if (data.getDesc() != null && data.getDesc().toLowerCase().contains("thất bại")) {
            return false;
        }
        return data.getAmount() != null && data.getAmount() > 0;
    }

    public static OffsetDateTime parseTransactionDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDateTime local = LocalDateTime.parse(value.trim(), PAYOS_DATETIME);
            return local.atOffset(ZoneOffset.ofHours(7));
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
