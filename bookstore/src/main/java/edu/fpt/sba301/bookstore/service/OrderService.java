package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.CheckoutRequest;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.payment.WebhookResult;
import org.springframework.data.domain.Page;

public interface OrderService {
    CheckoutResult checkout(User user, CheckoutRequest request, String idempotencyKey, String returnUrl);

    OrderResponse handlePaymentWebhook(String provider, WebhookResult result);

    OrderResponse cancelOrder(User user, Long orderId);

    void processExpiredPendingOrders();

    Page<OrderResponse> getOrderHistory(User user, int page, int size);

    OrderResponse getOrderDetail(User user, Long orderId);
}
