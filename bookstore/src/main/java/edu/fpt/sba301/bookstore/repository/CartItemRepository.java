package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteAllByBookId(Long bookId);
}
