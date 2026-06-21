package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByBookId(Long bookId);

    List<OrderItem> findByOrder(Order order);

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            SELECT COUNT(oi) > 0 FROM OrderItem oi
            JOIN oi.order o
            WHERE o.user.id = :userId AND oi.book.id = :bookId AND o.status = 'DELIVERED'
            """)
    boolean existsDeliveredPurchase(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query(value = """
            SELECT oi.book_id AS bookId, b.title AS title, SUM(oi.quantity) AS soldCount
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN books b ON b.id = oi.book_id
            WHERE o.status <> 'CANCELLED'
              AND o.created_at >= :start AND o.created_at < :end
            GROUP BY oi.book_id, b.title
            ORDER BY soldCount DESC
            LIMIT 5
            """, nativeQuery = true)
    List<TopBookSalesProjection> findTopSellingBooks(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);
}
