package edu.fpt.sba301.bookstore.payment;

import edu.fpt.sba301.bookstore.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class PayOsPaymentService implements PaymentService {

    @Value("${app.payment.payos.client-id:}")
    private String clientId;

    @Value("${app.payment.payos.api-key:}")
    private String apiKey;

    @Value("${app.payment.payos.checksum-key:}")
    private String checksumKey;

    @Override
    public PaymentResponse createPaymentUrl(Order order, String returnUrl) {
        if (clientId == null || clientId.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("PayOS is not configured. Set PAYOS_CLIENT_ID and PAYOS_API_KEY.");
        }
        log.warn("PayOS skeleton: createPaymentUrl called for order {} — not fully implemented", order.getId());
        return new PaymentResponse("https://pay.payos.vn/web/" + order.getId(), "PAYOS-SKELETON-" + order.getId());
    }

    @Override
    public WebhookResult verifyWebhook(Map<String, String> params) {
        if (checksumKey == null || checksumKey.isBlank()) {
            return new WebhookResult(false, null, null, null, "PayOS checksum key not configured");
        }
        log.warn("PayOS skeleton: verifyWebhook called with params {} — not fully implemented", params.keySet());
        return new WebhookResult(false, null, null, null, "PayOS webhook verification not implemented");
    }

    @Override
    public String getProviderName() {
        return "payos";
    }
}
