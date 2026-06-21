package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.constant.LoyaltyConstants;
import edu.fpt.sba301.bookstore.constant.PointTransactionReason;
import edu.fpt.sba301.bookstore.dto.response.PointTransactionResponse;
import edu.fpt.sba301.bookstore.dto.response.PointsResponse;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.PointTransaction;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.PointTransactionRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.service.PointService;
import edu.fpt.sba301.bookstore.support.PaginationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Override
    public long calculateMaxRedeemablePoints(User user, long subtotal) {
        long maxByPercent = (long) Math.floor(subtotal * LoyaltyConstants.MAX_REDEEM_RATIO / LoyaltyConstants.VND_PER_POINT);
        return Math.min(user.getPoints(), maxByPercent);
    }

    @Override
    public long calculatePointsDiscount(long pointsToRedeem) {
        return pointsToRedeem * LoyaltyConstants.VND_PER_POINT;
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
        saveTransaction(user, order, -pointsToRedeem, PointTransactionReason.POINTS_REDEEMED);
        user.setPoints(user.getPoints() - pointsToRedeem);
    }

    @Override
    @Transactional
    public void refundRedeemedPoints(Order order) {
        if (order.getPointsUsed() == null || order.getPointsUsed() <= 0) {
            return;
        }
        userRepository.adjustPoints(order.getUser().getId(), order.getPointsUsed());
        saveTransaction(order.getUser(), order, order.getPointsUsed(), PointTransactionReason.POINTS_REFUNDED);
    }

    @Override
    @Transactional
    public void creditOnDelivered(Order order) {
        long earned = order.getTotal() / LoyaltyConstants.POINTS_EARNED_VND_DIVISOR;
        if (earned <= 0) {
            return;
        }
        User user = order.getUser();
        userRepository.adjustPoints(user.getId(), earned);
        userRepository.adjustLifetimePoints(user.getId(), earned);
        saveTransaction(user, order, earned, PointTransactionReason.ORDER_DELIVERED);
        order.setPointsEarned(earned);
    }

    @Override
    @Transactional
    public void debitOnCancelAfterDelivered(Order order) {
        if (order.getPointsEarned() != null && order.getPointsEarned() > 0) {
            User user = order.getUser();
            userRepository.adjustPoints(user.getId(), -order.getPointsEarned());
            userRepository.adjustLifetimePoints(user.getId(), -order.getPointsEarned());
            saveTransaction(user, order, -order.getPointsEarned(), PointTransactionReason.ORDER_CANCELLED);
        }
        refundRedeemedPoints(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PointsResponse getPointsHistory(User user, int page, int size) {
        Page<PointTransaction> result = pointTransactionRepository.findByUserOrderByCreatedAtDesc(
                user, PaginationSupport.pageRequest(page, size));
        List<PointTransactionResponse> transactions = result.getContent().stream()
                .map(this::toTransactionResponse)
                .toList();
        User refreshed = userRepository.findById(user.getId()).orElse(user);
        return new PointsResponse(
                refreshed.getPoints(),
                transactions,
                PaginationSupport.normalizePage(page),
                PaginationSupport.normalizeSize(size),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void saveTransaction(User user, Order order, long delta, String reason) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setOrder(order);
        transaction.setDelta(delta);
        transaction.setReason(reason);
        transaction.setCreatedAt(OffsetDateTime.now());
        pointTransactionRepository.save(transaction);
    }

    private PointTransactionResponse toTransactionResponse(PointTransaction transaction) {
        return new PointTransactionResponse(
                transaction.getId(),
                transaction.getDelta(),
                transaction.getReason(),
                transaction.getOrder() != null ? transaction.getOrder().getId() : null,
                transaction.getCreatedAt()
        );
    }
}
