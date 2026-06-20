package edu.fpt.sba301.bookstore.payment;

import edu.fpt.sba301.bookstore.entity.Order;

import java.util.Map;

public interface PaymentService {
    PaymentResponse createPaymentUrl(Order order, String returnUrl);

    WebhookResult verifyWebhook(Map<String, String> params);

    String getProviderName();
}
