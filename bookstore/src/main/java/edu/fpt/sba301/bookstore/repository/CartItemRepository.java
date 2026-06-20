package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Cart;
import edu.fpt.sba301.bookstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteAllByBookId(Long bookId);

    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByCartAndBookId(Cart cart, Long bookId);

    void deleteByCart(Cart cart);
}
