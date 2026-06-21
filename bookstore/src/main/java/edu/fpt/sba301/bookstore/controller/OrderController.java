package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.CheckoutRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.service.CheckoutResult;
import edu.fpt.sba301.bookstore.service.OrderService;
import edu.fpt.sba301.bookstore.support.ApiResponseSupport;
import edu.fpt.sba301.bookstore.support.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            Principal principal,
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam(value = "returnUrl", required = false, defaultValue = "http://localhost:3000/payment/return") String returnUrl) {
        CheckoutResult result = orderService.checkout(
                currentUserService.requireUser(principal),
                request,
                idempotencyKey,
                returnUrl);
        if (result.newlyCreated()) {
            return ApiResponseSupport.created(result.order());
        }
        return ApiResponseSupport.ok(result.order());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOrders(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> orders = orderService.getOrderHistory(currentUserService.requireUser(principal), page, size);
        Map<String, Object> data = new HashMap<>();
        data.put("items", orders.getContent());
        data.put("page", orders.getNumber());
        data.put("size", orders.getSize());
        data.put("totalElements", orders.getTotalElements());
        data.put("totalPages", orders.getTotalPages());
        return ApiResponseSupport.ok(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            Principal principal,
            @PathVariable Long id) {
        OrderResponse order = orderService.getOrderDetail(currentUserService.requireUser(principal), id);
        return ApiResponseSupport.ok(order);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            Principal principal,
            @PathVariable Long id) {
        OrderResponse order = orderService.cancelOrder(currentUserService.requireUser(principal), id);
        return ApiResponseSupport.ok(order);
    }
}
