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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
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
    public ResponseEntity<?> webhookGet(
            @PathVariable String provider,
            @RequestParam Map<String, String> params) {
        return handleWebhook(provider, params, null, true);
    }

    @PostMapping("/{provider}")
    public ResponseEntity<?> webhookPost(
            @PathVariable String provider,
            @RequestParam(required = false) Map<String, String> queryParams,
            @RequestBody(required = false) Object body) {
        Map<String, String> params = new HashMap<>();
        if (queryParams != null) {
            params.putAll(queryParams);
        }
        return handleWebhook(provider, params, body, false);
    }

    private ResponseEntity<?> handleWebhook(
            String provider,
            Map<String, String> params,
            Object body,
            boolean browserGet) {
        PaymentService paymentService = paymentServiceFactory.getService(provider);
        WebhookResult result = paymentService.verifyWebhook(params, body);
        try {
            OrderResponse order = orderService.handlePaymentWebhook(provider, result);
            log.info("Payment webhook provider={} orderId={} success={}", provider, result.orderId(), result.success());

            String returnUrl = params.get("returnUrl");
            if (browserGet
                    && "mock".equalsIgnoreCase(provider)
                    && result.success()
                    && returnUrl != null
                    && !returnUrl.isBlank()
                    && isAllowedRedirect(returnUrl)) {
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(returnUrl)).build();
            }

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

    private boolean isAllowedRedirect(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || uri.getScheme() == null) {
                return false;
            }
            boolean localHost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
            boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            return localHost && httpScheme;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
