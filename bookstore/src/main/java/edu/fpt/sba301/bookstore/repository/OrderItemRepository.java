package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByBookId(Long bookId);
}
