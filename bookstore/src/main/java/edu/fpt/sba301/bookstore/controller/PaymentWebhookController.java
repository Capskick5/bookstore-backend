package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.payment.PaymentService;
import edu.fpt.sba301.bookstore.payment.PaymentServiceFactory;
import edu.fpt.sba301.bookstore.payment.WebhookResult;
import edu.fpt.sba301.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentServiceFactory paymentServiceFactory;
    private final OrderService orderService;

    @GetMapping("/{provider}")
    public ResponseEntity<ApiResponse<OrderResponse>> webhookGet(
            @PathVariable String provider,
            @RequestParam Map<String, String> params) {
        return handleWebhook(provider, params);
    }

    @PostMapping("/{provider}")
    public ResponseEntity<ApiResponse<OrderResponse>> webhookPost(
            @PathVariable String provider,
            @RequestParam(required = false) Map<String, String> queryParams,
            @RequestBody(required = false) Map<String, String> body) {
        Map<String, String> params = new HashMap<>();
        if (queryParams != null) {
            params.putAll(queryParams);
        }
        if (body != null) {
            params.putAll(body);
        }
        return handleWebhook(provider, params);
    }

    private ResponseEntity<ApiResponse<OrderResponse>> handleWebhook(String provider, Map<String, String> params) {
        PaymentService paymentService = paymentServiceFactory.getService(provider);
        WebhookResult result = paymentService.verifyWebhook(params);
        OrderResponse order = orderService.handlePaymentWebhook(provider, result);
        ApiResponse<OrderResponse> response = new ApiResponse<>();
        response.setCode(result.success() ? 200 : 400);
        response.setMessage(result.message());
        response.setData(order);
        return ResponseEntity.ok(response);
    }
}
