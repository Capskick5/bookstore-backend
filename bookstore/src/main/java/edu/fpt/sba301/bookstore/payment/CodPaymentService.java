package edu.fpt.sba301.bookstore.payment;

import edu.fpt.sba301.bookstore.constant.PaymentMethods;
import edu.fpt.sba301.bookstore.entity.Order;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CodPaymentService implements PaymentService {

    @Override
    public PaymentResponse createPaymentUrl(Order order, String returnUrl) {
        String transactionId = order.getId() != null ? "COD-" + order.getId() : "COD-PENDING";
        return new PaymentResponse(null, transactionId);
    }

    @Override
    public WebhookResult verifyWebhook(Map<String, String> params, Object requestBody) {
        return new WebhookResult(false, null, null, null, "COD does not use webhooks", null);
    }

    @Override
    public String getProviderName() {
        return PaymentMethods.COD;
    }
}
