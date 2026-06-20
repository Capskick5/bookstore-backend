package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.CartItemRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.CartResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Principal principal) {
        User user = currentUser(principal);
        CartResponse cart = cartService.getCart(user);
        return ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            Principal principal,
            @Valid @RequestBody CartItemRequest request) {
        User user = currentUser(principal);
        CartResponse cart = cartService.addItem(user, request.bookId(), request.quantity());
        return ok(cart);
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            Principal principal,
            @PathVariable Long bookId,
            @Valid @RequestBody CartItemRequest request) {
        User user = currentUser(principal);
        CartResponse cart = cartService.updateItem(user, bookId, request.quantity());
        return ok(cart);
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            Principal principal,
            @PathVariable Long bookId) {
        User user = currentUser(principal);
        CartResponse cart = cartService.removeItem(user, bookId);
        return ok(cart);
    }

    private User currentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ResponseEntity<ApiResponse<CartResponse>> ok(CartResponse data) {
        ApiResponse<CartResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }
}
