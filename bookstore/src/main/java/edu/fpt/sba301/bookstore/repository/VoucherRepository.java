package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Voucher v SET v.usedCount = v.usedCount + 1
            WHERE v.id = :id AND v.active = true
              AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)
            """)
    int incrementUsedCountIfAllowed(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 WHERE v.id = :id AND v.usedCount > 0")
    int decrementUsedCount(@Param("id") Long id);
}
