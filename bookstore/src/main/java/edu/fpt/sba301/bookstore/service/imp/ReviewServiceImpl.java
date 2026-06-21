package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.request.ReviewRequest;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.dto.response.ReviewResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Review;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.OrderItemRepository;
import edu.fpt.sba301.bookstore.repository.ReviewRepository;
import edu.fpt.sba301.bookstore.service.ReviewService;
import edu.fpt.sba301.bookstore.support.PaginationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(User user, Long bookId, ReviewRequest request) {
        Book book = bookRepository.findByIdAndActiveTrue(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        if (reviewRepository.existsByBookIdAndUserId(bookId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this book");
        }

        if (!orderItemRepository.existsDeliveredPurchase(user.getId(), bookId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only customers who received this book can leave a review");
        }

        Review review = new Review();
        review.setBook(book);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setCreatedAt(OffsetDateTime.now());
        review = reviewRepository.save(review);

        bookRepository.recomputeRatingAvg(bookId);
        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listReviews(Long bookId, int page, int size) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }
        Page<ReviewResponse> reviews = reviewRepository
                .findByBookIdOrderByCreatedAtDesc(bookId, PaginationSupport.pageRequest(page, size))
                .map(this::toResponse);
        return PageResponse.from(reviews);
    }

    @Override
    @Transactional
    public void deleteReview(User user, Long reviewId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        if (!isAdmin && !review.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Long bookId = review.getBook().getId();
        reviewRepository.delete(review);
        bookRepository.recomputeRatingAvg(bookId);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBook().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
