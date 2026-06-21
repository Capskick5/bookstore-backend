package edu.fpt.sba301.bookstore.service.imp;

import tools.jackson.databind.json.JsonMapper;
import edu.fpt.sba301.bookstore.constant.LoyaltyConstants;
import edu.fpt.sba301.bookstore.constant.VoucherTypes;
import edu.fpt.sba301.bookstore.dto.request.CheckoutRequest;
import edu.fpt.sba301.bookstore.dto.response.OrderResponse;
import edu.fpt.sba301.bookstore.entity.*;
import edu.fpt.sba301.bookstore.enums.OrderStatus;
import edu.fpt.sba301.bookstore.enums.VoucherRedemptionStatus;
import edu.fpt.sba301.bookstore.mapper.OrderMapper;
import edu.fpt.sba301.bookstore.payment.PaymentResponse;
import edu.fpt.sba301.bookstore.payment.PaymentService;
import edu.fpt.sba301.bookstore.payment.PaymentServiceFactory;
import edu.fpt.sba301.bookstore.payment.WebhookResult;
import edu.fpt.sba301.bookstore.repository.*;
import edu.fpt.sba301.bookstore.service.CartService;
import edu.fpt.sba301.bookstore.service.CheckoutResult;
import edu.fpt.sba301.bookstore.service.OrderService;
import edu.fpt.sba301.bookstore.service.PointService;
import edu.fpt.sba301.bookstore.support.PaginationSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final JsonMapper jsonMapper;

    @Value("${app.order.stock-hold-minutes:15}")
    private int stockHoldMinutes;

    @Value("${app.order.shipping-fee:30000}")
    private long shippingFeeFlat;

    @Value("${app.order.free-shipping-threshold:300000}")
    private long freeShippingThreshold;

    @Override
    @Transactional
    public CheckoutResult checkout(User user, CheckoutRequest request, String idempotencyKey, String returnUrl) {
        Optional<CheckoutResult> existing = findExistingCheckout(idempotencyKey, returnUrl);
        if (existing.isPresent()) {
            return existing.get();
        }

        ensureNotCombiningVoucherAndPoints(request);
        Cart cart = requireNonEmptyCart(user);
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        Address address = requireOwnedAddress(user, request.addressId());

        ValidatedCart validatedCart = validateCartItems(cartItems);
        OrderPricing pricing = calculatePricing(user, request, validatedCart.subtotal());

        reserveStock(validatedCart.items());
        OffsetDateTime now = OffsetDateTime.now();
        Order order = persistPendingOrder(user, request, idempotencyKey, address, validatedCart, pricing, now);
        persistOrderItems(order, validatedCart.items());
        reserveVoucherIfPresent(user, order, pricing, now);
        redeemPointsIfPresent(user, order, pricing.pointsToRedeem());

        PaymentService paymentService = paymentServiceFactory.getActiveService();
        PaymentResponse payment = paymentService.createPaymentUrl(order, returnUrl);
        order.setPaymentTransactionId(payment.transactionId());
        order = orderRepository.save(order);

        log.info("Checkout created pending order id={} userId={} total={}", order.getId(), user.getId(), order.getTotal());
        return new CheckoutResult(orderMapper.toResponse(order, payment.paymentUrl()), true);
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
            return orderMapper.toResponse(order, null);
        }

        ensurePaymentAmountMatches(order, result.amount());

        if (OrderStatus.PAID.equals(order.getStatus())) {
            return orderMapper.toResponse(order, null);
        }

        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            return handleLatePaymentWebhook(order, result);
        }

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not pending payment");
        }

        return completePaidOrder(order, result.transactionId());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(User user, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        ensureOrderOwner(order, user);
        ensureOrderIsCancellable(order);
        cancelOrderInternal(order, "user_cancelled");
        return orderMapper.toResponse(order, null);
    }

    @Override
    @Transactional
    public void processExpiredPendingOrders() {
        List<Order> expired = orderRepository.findExpiredPendingOrders(OffsetDateTime.now());
        for (Order order : expired) {
            orderRepository.findByIdForUpdate(order.getId()).ifPresent(current -> {
                if (OrderStatus.PENDING.equals(current.getStatus())) {
                    cancelOrderInternal(current, "timeout");
                }
            });
        }
        if (!expired.isEmpty()) {
            log.info("Processed {} expired pending order(s)", expired.size());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrderHistory(User user, int page, int size) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user, PaginationSupport.pageRequest(page, size))
                .map(order -> orderMapper.toResponse(order, null));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        ensureOrderOwner(order, user);
        return orderMapper.toResponse(order, null);
    }

    private Optional<CheckoutResult> findExistingCheckout(String idempotencyKey, String returnUrl) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(order -> {
                    String paymentUrl = null;
                    if (OrderStatus.PENDING.equals(order.getStatus())) {
                        PaymentService paymentService = paymentServiceFactory.getActiveService();
                        PaymentResponse payment = paymentService.createPaymentUrl(order, returnUrl);
                        paymentUrl = payment.paymentUrl();
                        order.setPaymentTransactionId(payment.transactionId());
                        orderRepository.save(order);
                    }
                    return new CheckoutResult(orderMapper.toResponse(order, paymentUrl), false);
                });
    }

    private void ensureNotCombiningVoucherAndPoints(CheckoutRequest request) {
        boolean hasVoucher = hasText(request.voucherCode());
        long pointsToRedeem = defaultPointsToRedeem(request.pointsToRedeem());
        if (hasVoucher && pointsToRedeem > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot combine points and voucher.");
        }
    }

    private Cart requireNonEmptyCart(User user) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));
        if (cartItemRepository.findByCart(cart).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }
        return cart;
    }

    private Address requireOwnedAddress(User user, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipping address"));
    }

    private ValidatedCart validateCartItems(List<CartItem> cartItems) {
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
        return new ValidatedCart(validItems, subtotal);
    }

    private OrderPricing calculatePricing(User user, CheckoutRequest request, long subtotal) {
        boolean hasVoucher = hasText(request.voucherCode());
        long pointsToRedeem = defaultPointsToRedeem(request.pointsToRedeem());

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
        return new OrderPricing(voucher, discount, shippingFee, total, pointsToRedeem);
    }

    private void reserveStock(List<CartItem> items) {
        for (CartItem item : items) {
            int updated = bookRepository.reserveStock(item.getBook().getId(), item.getQuantity());
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for book: " + item.getBook().getTitle());
            }
        }
    }

    private Order persistPendingOrder(
            User user,
            CheckoutRequest request,
            String idempotencyKey,
            Address address,
            ValidatedCart validatedCart,
            OrderPricing pricing,
            OffsetDateTime now) {
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(validatedCart.subtotal());
        order.setDiscount(pricing.discount());
        order.setShippingFee(pricing.shippingFee());
        order.setTotal(pricing.total());
        order.setAddressSnapshot(buildAddressSnapshot(address));
        order.setPaymentMethod(request.paymentMethod());
        order.setVoucherCode(pricing.voucher() != null ? pricing.voucher().getCode() : null);
        order.setPointsUsed(pricing.pointsToRedeem());
        order.setPointsEarned(pricing.total() / LoyaltyConstants.POINTS_EARNED_VND_DIVISOR);
        order.setIdempotencyKey(idempotencyKey);
        order.setExpiresAt(now.plusMinutes(stockHoldMinutes));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setManualRefundRequired(false);
        return orderRepository.save(order);
    }

    private void persistOrderItems(Order order, List<CartItem> items) {
        for (CartItem item : items) {
            Book book = item.getBook();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(book);
            orderItem.setTitleSnapshot(book.getTitle());
            orderItem.setUnitPrice(book.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItemRepository.save(orderItem);
        }
    }

    private void reserveVoucherIfPresent(User user, Order order, OrderPricing pricing, OffsetDateTime now) {
        if (pricing.voucher() == null) {
            return;
        }
        int incremented = voucherRepository.incrementUsedCountIfAllowed(pricing.voucher().getId());
        if (incremented == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher usage limit exceeded");
        }
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucher(pricing.voucher());
        redemption.setUser(user);
        redemption.setOrder(order);
        redemption.setStatus(VoucherRedemptionStatus.PENDING);
        redemption.setCreatedAt(now);
        voucherRedemptionRepository.save(redemption);
    }

    private void redeemPointsIfPresent(User user, Order order, long pointsToRedeem) {
        if (pointsToRedeem > 0) {
            pointService.redeemAtCheckout(user, order, pointsToRedeem);
        }
    }

    private OrderResponse handleLatePaymentWebhook(Order order, WebhookResult result) {
        OffsetDateTime cancelledAt = order.getUpdatedAt();
        OffsetDateTime authorizedAt = result.authorizedAt();
        if (authorizedAt == null || cancelledAt == null || authorizedAt.isAfter(cancelledAt)) {
            flagManualRefund(order);
            log.warn("Late payment webhook flagged manual refund for orderId={}", order.getId());
            return orderMapper.toResponse(order, null);
        }

        if (!reserveStockForOrder(order)) {
            flagManualRefund(order);
            log.warn("Late payment webhook could not reserve stock for orderId={}", order.getId());
            return orderMapper.toResponse(order, null);
        }

        if (!reapplyOrderBenefitsAfterLateCancel(order)) {
            flagManualRefund(order);
            log.warn("Late payment webhook could not reapply voucher/points for orderId={}", order.getId());
            return orderMapper.toResponse(order, null);
        }

        log.info("Late payment webhook restored orderId={} to PAID", order.getId());
        return completePaidOrder(order, result.transactionId());
    }

    private OrderResponse completePaidOrder(Order order, String transactionId) {

        order.setStatus(OrderStatus.PAID);
        order.setPaymentTransactionId(transactionId);
        order.setManualRefundRequired(false);
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);

        applyPaidOrderSideEffects(order);
        return orderMapper.toResponse(order, null);
    }

    private boolean reserveStockForOrder(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            int updated = bookRepository.reserveStock(item.getBook().getId(), item.getQuantity());
            if (updated == 0) {
                return false;
            }
        }
        return true;
    }

    private void applyPaidOrderSideEffects(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            bookRepository.incrementSoldCount(item.getBook().getId(), item.getQuantity());
        }

        voucherRedemptionRepository.findByOrderId(order.getId()).ifPresent(redemption -> {
            redemption.setStatus(VoucherRedemptionStatus.ACTIVE);
            voucherRedemptionRepository.save(redemption);
        });

        cartService.clearCart(order.getUser());
    }

    private void flagManualRefund(Order order) {
        order.setManualRefundRequired(true);
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);
    }

    private void cancelOrderInternal(Order order, String reason) {
        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            return;
        }
        boolean wasPaid = OrderStatus.PAID.equals(order.getStatus());
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            bookRepository.restoreStock(item.getBook().getId(), item.getQuantity());
            if (wasPaid) {
                bookRepository.decrementSoldCount(item.getBook().getId(), item.getQuantity());
            }
        }
        releaseVoucher(order);
        pointService.refundRedeemedPoints(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);
        log.info("Cancelled order id={} reason={}", order.getId(), reason);
    }

    private boolean reapplyOrderBenefitsAfterLateCancel(Order order) {
        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (hasText(order.getVoucherCode()) && voucherRedemptionRepository.findByOrderId(order.getId()).isEmpty()) {
            Voucher voucher = voucherRepository.findByCodeIgnoreCase(order.getVoucherCode()).orElse(null);
            if (voucher == null) {
                return false;
            }
            int incremented = voucherRepository.incrementUsedCountIfAllowed(voucher.getId());
            if (incremented == 0) {
                return false;
            }
            VoucherRedemption redemption = new VoucherRedemption();
            redemption.setVoucher(voucher);
            redemption.setUser(user);
            redemption.setOrder(order);
            redemption.setStatus(VoucherRedemptionStatus.PENDING);
            redemption.setCreatedAt(OffsetDateTime.now());
            voucherRedemptionRepository.save(redemption);
        }

        long pointsToRedeem = defaultPointsToRedeem(order.getPointsUsed());
        if (pointsToRedeem > 0) {
            try {
                pointService.redeemAtCheckout(user, order, pointsToRedeem);
            } catch (ResponseStatusException ex) {
                return false;
            }
        }
        return true;
    }

    private void releaseVoucher(Order order) {
        voucherRedemptionRepository.findByOrderId(order.getId()).ifPresent(redemption -> {
            voucherRepository.decrementUsedCount(redemption.getVoucher().getId());
            voucherRedemptionRepository.delete(redemption);
        });
    }

    private void ensureOrderOwner(Order order, User user) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void ensureOrderIsCancellable(Order order) {
        if (OrderStatus.SHIPPED.equals(order.getStatus())
                || OrderStatus.DELIVERED.equals(order.getStatus())
                || OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order can no longer be cancelled");
        }
    }

    private void ensurePaymentAmountMatches(Order order, Long amount) {
        if (!order.getTotal().equals(amount)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount mismatch");
        }
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
            case VoucherTypes.FIXED -> new DiscountResult(Math.min(voucher.getValue(), subtotal), false);
            case VoucherTypes.PERCENT -> {
                long raw = subtotal * voucher.getValue() / 100L;
                if (voucher.getMaxDiscount() != null) {
                    raw = Math.min(raw, voucher.getMaxDiscount());
                }
                yield new DiscountResult(Math.min(raw, subtotal), false);
            }
            case VoucherTypes.SHIP -> new DiscountResult(0L, true);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported voucher type");
        };
    }

    private long calculateShippingFee(long discountedSubtotal, boolean shipVoucher, Voucher voucher) {
        if (discountedSubtotal >= freeShippingThreshold) {
            return 0L;
        }
        if (shipVoucher || (voucher != null && VoucherTypes.SHIP.equals(voucher.getType()))) {
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
            return jsonMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return address.getLine() + ", " + address.getCity();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long defaultPointsToRedeem(Long pointsToRedeem) {
        return pointsToRedeem != null ? pointsToRedeem : 0L;
    }

    private record ValidatedCart(List<CartItem> items, long subtotal) {
    }

    private record OrderPricing(Voucher voucher, long discount, long shippingFee, long total, long pointsToRedeem) {
    }

    private record DiscountResult(long amount, boolean shipVoucher) {
    }
}
