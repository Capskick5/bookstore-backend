package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Review;
import edu.fpt.sba301.bookstore.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    Optional<Review> findByIdAndUserId(Long id, Long userId);
}
