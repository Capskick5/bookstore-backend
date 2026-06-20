package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Cart;
import edu.fpt.sba301.bookstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUserId(Long userId);
}
