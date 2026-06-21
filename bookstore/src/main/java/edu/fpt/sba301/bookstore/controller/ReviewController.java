package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.ReviewRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.dto.response.ReviewResponse;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.enums.Role;
import edu.fpt.sba301.bookstore.service.ReviewService;
import edu.fpt.sba301.bookstore.support.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/books/{bookId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> listReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        ApiResponse<PageResponse<ReviewResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(reviewService.listReviews(bookId, page, size));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/books/{bookId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long bookId,
            @Valid @RequestBody ReviewRequest request,
            Principal principal) {
        User user = currentUserService.requireUser(principal);
        ReviewResponse data = reviewService.createReview(user, bookId, request);

        ApiResponse<ReviewResponse> response = new ApiResponse<>();
        response.setCode(201);
        response.setMessage("Created");
        response.setData(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            Principal principal,
            Authentication authentication) {
        User user = currentUserService.requireUser(principal);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
        reviewService.deleteReview(user, reviewId, isAdmin);

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("Deleted");
        return ResponseEntity.ok(response);
    }
}
