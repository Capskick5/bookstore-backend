package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.ReviewRequest;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.dto.response.ReviewResponse;
import edu.fpt.sba301.bookstore.entity.User;

public interface ReviewService {
    ReviewResponse createReview(User user, Long bookId, ReviewRequest request);

    PageResponse<ReviewResponse> listReviews(Long bookId, int page, int size);

    void deleteReview(User user, Long reviewId, boolean isAdmin);
}
