package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.response.PointsResponse;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.User;

public interface PointService {
    long calculateMaxRedeemablePoints(User user, long subtotal);

    long calculatePointsDiscount(long pointsToRedeem);

    void redeemAtCheckout(User user, Order order, long pointsToRedeem);

    void refundRedeemedPoints(Order order);

    void creditOnDelivered(Order order);

    void debitOnCancelAfterDelivered(Order order);

    PointsResponse getPointsHistory(User user, int page, int size);
}
