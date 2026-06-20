package edu.fpt.sba301.bookstore.service.imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fpt.sba301.bookstore.dto.request.CheckoutRequest;
import edu.fpt.sba301.bookstore.dto.response.OrderItemResponse;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.entity.*;
import edu.fpt.sba301.bookstore.enums.OrderStatus;
import edu.fpt.sba301.bookstore.enums.VoucherRedemptionStatus;
import edu.fpt.sba301.bookstore.payment.PaymentResponse;
import edu.fpt.sba301.bookstore.payment.PaymentService;
import edu.fpt.sba301.bookstore.payment.PaymentServiceFactory;
import edu.fpt.sba301.bookstore.payment.WebhookResult;
import edu.fpt.sba301.bookstore.repository.*;
import edu.fpt.sba301.bookstore.service.CartService;
import edu.fpt.sba301.bookstore.service.OrderService;
import edu.fpt.sba301.bookstore.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final AddressRepository addressRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final PointService pointService;
    private final PaymentServiceFactory paymentServiceFactory;
    private final ObjectMapper objectMapper;

    @Value("${app.order.stock-hold-minutes:15}")
    private int stockHoldMinutes;

    @Value("${app.order.shipping-fee:30000}")
    private long shippingFeeFlat;

    @Value("${app.order.free-shipping-threshold:300000}")
    private long freeShippingThreshold;

    @Override
    @Transactional
    public OrderResponse checkout(User user, CheckoutRequest request, String idempotencyKey, String returnUrl) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get(), null);
            }
        }

        boolean hasVoucher = request.voucherCode() != null && !request.voucherCode().isBlank();
        long pointsToRedeem = request.pointsToRedeem() != null ? request.pointsToRedeem() : 0L;
        if (hasVoucher && pointsToRedeem > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot combine points and voucher.");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Address address = addressRepository.findByIdAndUserId(request.addressId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipping address"));

        long subtotal = 0L;
        List<CartItem> validItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            Book book = item.getBook();
            if (Boolean.FALSE.equals(book.getActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart contains inactive book: " + book.getTitle());
            }
            if (item.getQuantity() > book.getStock()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for book: " + book.getTitle());
            }
            subtotal += book.getPrice() * item.getQuantity();
            validItems.add(item);
        }

        Voucher voucher = null;
        long discount = 0L;
        boolean shipVoucher = false;
        if (hasVoucher) {
            voucher = validateVoucher(request.voucherCode(), user, subtotal);
            DiscountResult discountResult = calculateVoucherDiscount(voucher, subtotal);
            discount = discountResult.amount();
            shipVoucher = discountResult.shipVoucher();
        } else if (pointsToRedeem > 0) {
            long maxPoints = pointService.calculateMaxRedeemablePoints(user, subtotal);
            if (pointsToRedeem > maxPoints) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Points exceed allowed redemption limit");
            }
            discount = pointService.calculatePointsDiscount(pointsToRedeem);
        }

        long discountedSubtotal = Math.max(0, subtotal - discount);
        long shippingFee = calculateShippingFee(discountedSubtotal, shipVoucher, voucher);
        long total = discountedSubtotal + shippingFee;

        for (CartItem item : validItems) {
            int updated = bookRepository.reserveStock(item.getBook().getId(), item.getQuantity());
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for book: " + item.getBook().getTitle());
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setShippingFee(shippingFee);
        order.setTotal(total);
        order.setAddressSnapshot(buildAddressSnapshot(address));
        order.setPaymentMethod(request.paymentMethod());
        order.setVoucherCode(voucher != null ? voucher.getCode() : null);
        order.setPointsUsed(pointsToRedeem);
        order.setPointsEarned(total / 10000L);
        order.setIdempotencyKey(idempotencyKey);
        order.setExpiresAt(now.plusMinutes(stockHoldMinutes));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order = orderRepository.save(order);

        for (CartItem item : validItems) {
            Book book = item.getBook();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(book);
            orderItem.setTitleSnapshot(book.getTitle());
            orderItem.setUnitPrice(book.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItemRepository.save(orderItem);
        }

        if (voucher != null) {
            int incremented = voucherRepository.incrementUsedCountIfAllowed(voucher.getId());
            if (incremented == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher usage limit exceeded");
            }
            VoucherRedemption redemption = new VoucherRedemption();
            redemption.setVoucher(voucher);
            redemption.setUser(user);
            redemption.setOrder(order);
            redemption.setStatus(VoucherRedemptionStatus.PENDING);
            redemption.setCreatedAt(now);
            voucherRedemptionRepository.save(redemption);
        }

        if (pointsToRedeem > 0) {
            pointService.redeemAtCheckout(user, order, pointsToRedeem);
        }

        PaymentService paymentService = paymentServiceFactory.getActiveService();
        PaymentResponse payment = paymentService.createPaymentUrl(order, returnUrl);
        order.setPaymentTransactionId(payment.transactionId());
        order = orderRepository.save(order);

        return toResponse(order, payment.paymentUrl());
    }

    @Override
    @Transactional
    public OrderResponse handlePaymentWebhook(String provider, WebhookResult result) {
        if (result.orderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.message());
        }
        Order order = orderRepository.findByIdForUpdate(result.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!result.success()) {
            if (OrderStatus.PENDING.equals(order.getStatus())) {
                cancelOrderInternal(order, "payment_failed");
            }
            return toResponse(order, null);
        }

        if (!order.getTotal().equals(result.amount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount mismatch");
        }

        if (OrderStatus.PAID.equals(order.getStatus())) {
            return toResponse(order, null);
        }

        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            return toResponse(order, null);
        }

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not pending payment");
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentTransactionId(result.transactionId());
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            bookRepository.incrementSoldCount(item.getBook().getId(), item.getQuantity());
        }

        voucherRedemptionRepository.findByOrderId(order.getId()).ifPresent(redemption -> {
            redemption.setStatus(VoucherRedemptionStatus.ACTIVE);
            voucherRedemptionRepository.save(redemption);
        });

        cartService.clearCart(order.getUser());
        return toResponse(order, null);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(User user, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (OrderStatus.SHIPPED.equals(order.getStatus())
                || OrderStatus.DELIVERED.equals(order.getStatus())
                || OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order can no longer be cancelled");
        }
        if (OrderStatus.PENDING.equals(order.getStatus()) || OrderStatus.PAID.equals(order.getStatus())) {
            cancelOrderInternal(order, "user_cancelled");
        }
        return toResponse(order, null);
    }

    @Override
    @Transactional
    public void processExpiredPendingOrders() {
        List<Order> expired = orderRepository.findExpiredPendingOrders(OffsetDateTime.now());
        for (Order order : expired) {
            orderRepository.findByIdForUpdate(order.getId()).ifPresent(o -> {
                if (OrderStatus.PENDING.equals(o.getStatus())) {
                    cancelOrderInternal(o, "timeout");
                }
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrderHistory(User user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return orderRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(safePage, safeSize))
                .map(order -> toResponse(order, null));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toResponse(order, null);
    }

    private void cancelOrderInternal(Order order, String reason) {
        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            return;
        }
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            bookRepository.restoreStock(item.getBook().getId(), item.getQuantity());
        }
        releaseVoucher(order);
        pointService.refundRedeemedPoints(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);
    }

    private void releaseVoucher(Order order) {
        voucherRedemptionRepository.findByOrderId(order.getId()).ifPresent(redemption -> {
            voucherRepository.decrementUsedCount(redemption.getVoucher().getId());
            voucherRedemptionRepository.delete(redemption);
        });
    }

    private Voucher validateVoucher(String code, User user, long subtotal) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid voucher code"));
        OffsetDateTime now = OffsetDateTime.now();
        if (Boolean.FALSE.equals(voucher.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is inactive");
        }
        if (voucher.getStartsAt() != null && voucher.getStartsAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is not yet active");
        }
        if (voucher.getEndsAt() != null && voucher.getEndsAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher has expired");
        }
        if (subtotal < voucher.getMinOrder()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order subtotal below voucher minimum");
        }
        long userUsage = voucherRedemptionRepository.countByVoucherAndUser(voucher, user);
        if (userUsage >= voucher.getPerUserLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher per-user limit exceeded");
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher usage limit exceeded");
        }
        return voucher;
    }

    private DiscountResult calculateVoucherDiscount(Voucher voucher, long subtotal) {
        return switch (voucher.getType()) {
            case "FIXED" -> new DiscountResult(Math.min(voucher.getValue(), subtotal), false);
            case "PERCENT" -> {
                long raw = subtotal * voucher.getValue() / 100L;
                if (voucher.getMaxDiscount() != null) {
                    raw = Math.min(raw, voucher.getMaxDiscount());
                }
                yield new DiscountResult(Math.min(raw, subtotal), false);
            }
            case "SHIP" -> new DiscountResult(0L, true);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported voucher type");
        };
    }

    private long calculateShippingFee(long discountedSubtotal, boolean shipVoucher, Voucher voucher) {
        if (discountedSubtotal >= freeShippingThreshold) {
            return 0L;
        }
        if (shipVoucher || (voucher != null && "SHIP".equals(voucher.getType()))) {
            return 0L;
        }
        return shippingFeeFlat;
    }

    private String buildAddressSnapshot(Address address) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("recipient", address.getRecipient());
            snapshot.put("phone", address.getPhone());
            snapshot.put("line", address.getLine());
            snapshot.put("city", address.getCity());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return address.getLine() + ", " + address.getCity();
        }
    }

    OrderResponse toResponse(Order order, String paymentUrl) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream()
                .map(i -> new OrderItemResponse(
                        i.getBook().getId(),
                        i.getTitleSnapshot(),
                        i.getUnitPrice(),
                        i.getQuantity()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingFee(),
                order.getTotal(),
                order.getVoucherCode(),
                order.getPointsUsed(),
                order.getPointsEarned(),
                paymentUrl,
                order.getExpiresAt(),
                order.getCreatedAt(),
                itemResponses
        );
    }

    private record DiscountResult(long amount, boolean shipVoucher) {
    }
}
