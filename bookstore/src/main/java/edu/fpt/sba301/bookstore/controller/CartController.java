package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.CartItemRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.CartResponse;
import edu.fpt.sba301.bookstore.service.CartService;
import edu.fpt.sba301.bookstore.support.ApiResponseSupport;
import edu.fpt.sba301.bookstore.support.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;

import static edu.fpt.sba301.bookstore.config.SwaggerConfig.BEARER_AUTH;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations")
@SecurityRequirement(name = BEARER_AUTH)
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Get the current user's cart")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Principal principal) {
        CartResponse cart = cartService.getCart(currentUserService.requireUser(principal));
        return ApiResponseSupport.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            Principal principal,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse cart = cartService.addItem(
                currentUserService.requireUser(principal),
                request.bookId(),
                request.quantity());
        return ApiResponseSupport.ok(cart);
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            Principal principal,
            @PathVariable Long bookId,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse cart = cartService.updateItem(
                currentUserService.requireUser(principal),
                bookId,
                request.quantity());
        return ApiResponseSupport.ok(cart);
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            Principal principal,
            @PathVariable Long bookId) {
        CartResponse cart = cartService.removeItem(currentUserService.requireUser(principal), bookId);
        return ApiResponseSupport.ok(cart);
    }
}
