package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.CheckoutRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            Principal principal,
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam(value = "returnUrl", required = false, defaultValue = "http://localhost:3000/payment/return") String returnUrl) {
        User user = currentUser(principal);
        OrderResponse order = orderService.checkout(user, request, idempotencyKey, returnUrl);
        ApiResponse<OrderResponse> response = new ApiResponse<>();
        response.setCode(201);
        response.setMessage("Created");
        response.setData(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOrders(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = currentUser(principal);
        Page<OrderResponse> orders = orderService.getOrderHistory(user, page, size);
        Map<String, Object> data = new HashMap<>();
        data.put("items", orders.getContent());
        data.put("page", orders.getNumber());
        data.put("size", orders.getSize());
        data.put("totalElements", orders.getTotalElements());
        data.put("totalPages", orders.getTotalPages());
        return ok(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            Principal principal,
            @PathVariable Long id) {
        User user = currentUser(principal);
        OrderResponse order = orderService.getOrderDetail(user, id);
        return ok(order);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            Principal principal,
            @PathVariable Long id) {
        User user = currentUser(principal);
        OrderResponse order = orderService.cancelOrder(user, id);
        return ok(order);
    }

    private User currentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }
}
