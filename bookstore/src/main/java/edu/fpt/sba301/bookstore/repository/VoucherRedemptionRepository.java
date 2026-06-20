package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.entity.Voucher;
import edu.fpt.sba301.bookstore.entity.VoucherRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {
    Optional<VoucherRedemption> findByOrderId(Long orderId);

    long countByVoucherAndUser(Voucher voucher, User user);

    void deleteByOrderId(Long orderId);
}
