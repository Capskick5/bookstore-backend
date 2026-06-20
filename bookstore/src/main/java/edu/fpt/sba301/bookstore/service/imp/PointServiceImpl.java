package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.response.PointTransactionResponse;
import edu.fpt.sba301.bookstore.dto.response.PointsResponse;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.PointTransaction;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.PointTransactionRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private static final long VND_PER_POINT = 100L;

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Override
    public long calculateMaxRedeemablePoints(User user, long subtotal) {
        long maxByPercent = (long) Math.floor(subtotal * 0.20 / VND_PER_POINT);
        return Math.min(user.getPoints(), maxByPercent);
    }

    @Override
    public long calculatePointsDiscount(long pointsToRedeem) {
        return pointsToRedeem * VND_PER_POINT;
    }

    @Override
    @Transactional
    public void redeemAtCheckout(User user, Order order, long pointsToRedeem) {
        if (pointsToRedeem <= 0) {
            return;
        }
        long maxRedeemable = calculateMaxRedeemablePoints(user, order.getSubtotal());
        if (pointsToRedeem > maxRedeemable) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Points exceed allowed redemption limit");
        }
        int updated = userRepository.adjustPoints(user.getId(), -pointsToRedeem);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient points");
        }
        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setDelta(-pointsToRedeem);
        tx.setReason("points_redeemed");
        tx.setCreatedAt(OffsetDateTime.now());
        pointTransactionRepository.save(tx);
        user.setPoints(user.getPoints() - pointsToRedeem);
    }

    @Override
    @Transactional
    public void refundRedeemedPoints(Order order) {
        if (order.getPointsUsed() == null || order.getPointsUsed() <= 0) {
            return;
        }
        User user = order.getUser();
        userRepository.adjustPoints(user.getId(), order.getPointsUsed());
        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setDelta(order.getPointsUsed());
        tx.setReason("points_refunded");
        tx.setCreatedAt(OffsetDateTime.now());
        pointTransactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void creditOnDelivered(Order order) {
        long earned = order.getTotal() / 10000L;
        if (earned <= 0) {
            return;
        }
        User user = order.getUser();
        userRepository.adjustPoints(user.getId(), earned);
        userRepository.adjustLifetimePoints(user.getId(), earned);
        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setDelta(earned);
        tx.setReason("order_delivered");
        tx.setCreatedAt(OffsetDateTime.now());
        pointTransactionRepository.save(tx);
        order.setPointsEarned(earned);
    }

    @Override
    @Transactional
    public void debitOnCancelAfterDelivered(Order order) {
        if (order.getPointsEarned() != null && order.getPointsEarned() > 0) {
            User user = order.getUser();
            userRepository.adjustPoints(user.getId(), -order.getPointsEarned());
            userRepository.adjustLifetimePoints(user.getId(), -order.getPointsEarned());
            PointTransaction tx = new PointTransaction();
            tx.setUser(user);
            tx.setOrder(order);
            tx.setDelta(-order.getPointsEarned());
            tx.setReason("order_cancelled");
            tx.setCreatedAt(OffsetDateTime.now());
            pointTransactionRepository.save(tx);
        }
        refundRedeemedPoints(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PointsResponse getPointsHistory(User user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<PointTransaction> result = pointTransactionRepository.findByUserOrderByCreatedAtDesc(
                user, PageRequest.of(safePage, safeSize));
        List<PointTransactionResponse> transactions = result.getContent().stream()
                .map(tx -> new PointTransactionResponse(
                        tx.getId(),
                        tx.getDelta(),
                        tx.getReason(),
                        tx.getOrder() != null ? tx.getOrder().getId() : null,
                        tx.getCreatedAt()))
                .toList();
        User refreshed = userRepository.findById(user.getId()).orElse(user);
        return new PointsResponse(
                refreshed.getPoints(),
                transactions,
                safePage,
                safeSize,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
