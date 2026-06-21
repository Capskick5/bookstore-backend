package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.UpdateOrderStatusRequest;
import edu.fpt.sba301.bookstore.dto.response.AdminOrderResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.service.OrderService;
import edu.fpt.sba301.bookstore.support.ApiResponseSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

import static edu.fpt.sba301.bookstore.config.SwaggerConfig.BEARER_AUTH;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Orders", description = "Admin order management")
@SecurityRequirement(name = BEARER_AUTH)
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<?> listOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String status) {
        Page<AdminOrderResponse> orders = orderService.getAdminOrders(page, size, status);
        Map<String, Object> data = new HashMap<>();
        data.put("items", orders.getContent());
        data.put("page", orders.getNumber());
        data.put("size", orders.getSize());
        data.put("totalElements", orders.getTotalElements());
        data.put("totalPages", orders.getTotalPages());
        return ApiResponseSupport.ok(data);
    }

    @Operation(summary = "Get full order detail for admin review")
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        AdminOrderResponse order = orderService.getAdminOrderDetail(id);
        return ApiResponseSupport.ok(order);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirmCodOrder(@PathVariable Long id) {
        OrderResponse order = orderService.confirmCodOrder(id);
        return ApiResponseSupport.ok(order);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ApiResponseSupport.ok(order);
    }
}
