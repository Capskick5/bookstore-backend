package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.payment.PaymentService;
import edu.fpt.sba301.bookstore.payment.PaymentServiceFactory;
import edu.fpt.sba301.bookstore.payment.WebhookResult;
import edu.fpt.sba301.bookstore.service.OrderService;
import edu.fpt.sba301.bookstore.support.ApiResponseSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentServiceFactory paymentServiceFactory;
    private final OrderService orderService;

    @GetMapping("/{provider}")
    public ResponseEntity<ApiResponse<OrderResponse>> webhookGet(
            @PathVariable String provider,
            @RequestParam Map<String, String> params) {
        return handleWebhook(provider, params, null);
    }

    @PostMapping("/{provider}")
    public ResponseEntity<ApiResponse<OrderResponse>> webhookPost(
            @PathVariable String provider,
            @RequestParam(required = false) Map<String, String> queryParams,
            @RequestBody(required = false) Object body) {
        Map<String, String> params = new HashMap<>();
        if (queryParams != null) {
            params.putAll(queryParams);
        }
        return handleWebhook(provider, params, body);
    }

    private ResponseEntity<ApiResponse<OrderResponse>> handleWebhook(
            String provider,
            Map<String, String> params,
            Object body) {
        PaymentService paymentService = paymentServiceFactory.getService(provider);
        WebhookResult result = paymentService.verifyWebhook(params, body);
        try {
            OrderResponse order = orderService.handlePaymentWebhook(provider, result);
            log.info("Payment webhook provider={} orderId={} success={}", provider, result.orderId(), result.success());
            return ResponseEntity.ok(ApiResponseSupport.envelope(
                    result.success() ? 200 : 400,
                    result.message(),
                    order));
        } catch (ResponseStatusException ex) {
            // PayOS kiểm tra Webhook URL cần HTTP 2xx; luôn trả 200, lỗi nằm trong body.
            log.warn("Payment webhook provider={} orderId={} httpStatus={} reason={}",
                    provider, result.orderId(), ex.getStatusCode().value(), ex.getReason());
            return ResponseEntity.ok(ApiResponseSupport.envelope(
                    ex.getStatusCode().value(),
                    ex.getReason(),
                    null));
        }
    }
}
