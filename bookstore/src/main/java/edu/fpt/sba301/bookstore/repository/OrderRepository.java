package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND LOWER(o.paymentMethod) <> 'cod' "
            + "AND o.expiresAt IS NOT NULL AND o.expiresAt < :now")
    List<Order> findExpiredPendingOrders(@Param("now") OffsetDateTime now);

    @Query("""
            SELECT COALESCE(SUM(o.total), 0) FROM Order o
            WHERE o.status <> 'CANCELLED'
              AND o.createdAt >= :start AND o.createdAt < :end
            """)
    Long sumRevenueBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.status <> 'CANCELLED'
              AND o.createdAt >= :start AND o.createdAt < :end
            """)
    long countNonCancelledBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
