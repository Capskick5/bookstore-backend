package edu.fpt.sba301.bookstore;

import tools.jackson.databind.json.JsonMapper;
import edu.fpt.sba301.bookstore.dto.request.CartItemRequest;
import edu.fpt.sba301.bookstore.dto.request.CheckoutRequest;
import edu.fpt.sba301.bookstore.dto.request.GuestCartItemRequest;
import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.enums.OrderStatus;
import edu.fpt.sba301.bookstore.payment.MockPaymentService;
import edu.fpt.sba301.bookstore.repository.AddressRepository;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.CartItemRepository;
import edu.fpt.sba301.bookstore.repository.CartRepository;
import edu.fpt.sba301.bookstore.repository.OrderRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.repository.VoucherRedemptionRepository;
import edu.fpt.sba301.bookstore.repository.VoucherRepository;
import edu.fpt.sba301.bookstore.service.CartService;
import edu.fpt.sba301.bookstore.service.OrderService;
import edu.fpt.sba301.bookstore.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartOrderTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PointService pointService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherRedemptionRepository voucherRedemptionRepository;

    private String customerToken;
    private Long bookId;
    private Long addressId;

    @BeforeEach
    void setUp() throws Exception {
        Book book = bookRepository.findAll().stream()
                .filter(b -> "Clean Code".equals(b.getTitle()))
                .findFirst()
                .orElseThrow();
        bookId = book.getId();
        book.setStock(50);
        book.setPrice(350000L);
        book.setOriginalPrice(400000L);
        book.setActive(true);
        bookRepository.save(book);

        bookRepository.findAll().stream()
                .filter(b -> "Atomic Habits".equals(b.getTitle()))
                .findFirst()
                .ifPresent(atomicHabits -> {
                    atomicHabits.setPrice(280000L);
                    atomicHabits.setOriginalPrice(320000L);
                    atomicHabits.setStock(100);
                    atomicHabits.setActive(true);
                    bookRepository.save(atomicHabits);
                });

        User customer = userRepository.findByEmail("test@example.com").orElseThrow();
        customer.setEnabled(true);
        customer.setPoints(50000L);
        customer.setLifetimePoints(50000L);
        userRepository.save(customer);
        addressId = addressRepository.findAllByUserId(customer.getId()).getFirst().getId();

        orderRepository.findByUserOrderByCreatedAtDesc(customer, Pageable.unpaged()).stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .forEach(order -> orderService.cancelOrder(customer, order.getId()));

        cartService.clearCart(customer);

        for (String code : List.of("SAVE50K", "PERCENT10", "FREESHIP")) {
            voucherRepository.findByCodeIgnoreCase(code).ifPresent(voucher -> {
                voucherRedemptionRepository.deleteByVoucherAndUser(voucher, customer);
                voucher.setUsedCount((int) voucherRedemptionRepository.countByVoucher(voucher));
                voucherRepository.save(voucher);
            });
        }

        customerToken = login("test@example.com", "password123", null);
    }

    @Test
    void cartCrudAndSubtotal() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CartItemRequest(bookId, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(700000))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(350000))
                .andExpect(jsonPath("$.data.items[0].coverUrl").exists());

        mockMvc.perform(put("/api/cart/items/" + bookId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CartItemRequest(bookId, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(350000));

        mockMvc.perform(delete("/api/cart/items/" + bookId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void rejectAddOverStock() throws Exception {
        Book book = bookRepository.findById(bookId).orElseThrow();
        int tooMany = book.getStock() + 1;
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CartItemRequest(bookId, tooMany))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void guestCartMergeOnLogin() throws Exception {
        Book inactive = bookRepository.findAll().stream()
                .filter(b -> "Inactive Book".equals(b.getTitle()))
                .findFirst()
                .orElseThrow();

        String token = login("test@example.com", "password123", List.of(
                new GuestCartItemRequest(bookId, 1),
                new GuestCartItemRequest(inactive.getId(), 2)
        ));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].bookId").value(bookId.intValue()));
    }

    @Test
    void checkoutCreatesPendingOrderWithPriceSnapshot() throws Exception {
        addBookToCart(1);
        Book bookBefore = bookRepository.findById(bookId).orElseThrow();
        int stockBefore = bookBefore.getStock();

        MvcResult result = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-test-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(bookBefore.getPrice()))
                .andExpect(jsonPath("$.data.paymentUrl").exists())
                .andReturn();

        Book bookAfter = bookRepository.findById(bookId).orElseThrow();
        assertEquals(stockBefore - 1, bookAfter.getStock());
        assertTrue(bookAfter.getStock() >= 0);
    }

    @Test
    void webhookMarksOrderPaidAndWebhookFailRestoresStock() throws Exception {
        addBookToCart(1);
        Book book = bookRepository.findById(bookId).orElseThrow();
        int stockBeforeCheckout = book.getStock();

        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-webhook-success"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> checkoutMap = jsonMapper.readValue(checkout.getResponse().getContentAsString(), Map.class);
        Map<?, ?> data = (Map<?, ?>) checkoutMap.get("data");
        Number orderId = (Number) data.get("id");
        Number total = (Number) data.get("total");
        String paymentUrl = (String) data.get("paymentUrl");
        assertNotNull(paymentUrl);

        String query = paymentUrl.substring(paymentUrl.indexOf('?') + 1);
        mockMvc.perform(get("/api/payment/webhook/mock?" + query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        Order paid = orderRepository.findById(orderId.longValue()).orElseThrow();
        assertEquals(OrderStatus.PAID, paid.getStatus());

        addBookToCart(1);
        MvcResult failCheckout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-webhook-fail"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> failMap = jsonMapper.readValue(failCheckout.getResponse().getContentAsString(), Map.class);
        Map<?, ?> failData = (Map<?, ?>) failMap.get("data");
        Long failOrderId = ((Number) failData.get("id")).longValue();
        Long failTotal = ((Number) failData.get("total")).longValue();
        String tx = "MOCK-FAIL-" + failOrderId;
        String signature = MockPaymentService.sign(failOrderId, failTotal, tx);

        mockMvc.perform(get("/api/payment/webhook/mock")
                        .param("orderId", failOrderId.toString())
                        .param("amount", failTotal.toString())
                        .param("transactionId", tx)
                        .param("signature", signature)
                        .param("status", "failed"))
                .andExpect(status().isOk());

        Order cancelled = orderRepository.findById(failOrderId).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        Book restored = bookRepository.findById(bookId).orElseThrow();
        assertEquals(stockBeforeCheckout - 1, restored.getStock());
    }

    @Test
    void orderHistoryDetailAndIdorProtection() throws Exception {
        addBookToCart(1);
        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-history"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Number orderId = extractOrderId(checkout);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(orderId.intValue()));

        String adminToken = login("admin@example.com", "adminpassword123", null);
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelPendingOrderRestoresStock() throws Exception {
        addBookToCart(1);
        Book book = bookRepository.findById(bookId).orElseThrow();
        int stockBeforeCheckout = book.getStock();

        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-cancel"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Number orderId = extractOrderId(checkout);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        Book restored = bookRepository.findById(bookId).orElseThrow();
        assertEquals(stockBeforeCheckout, restored.getStock());
    }

    @Test
    void expiredPendingOrderIsCancelledByScheduler() throws Exception {
        addBookToCart(1);
        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-timeout"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = extractOrderId(checkout).longValue();
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        orderRepository.save(order);

        orderService.processExpiredPendingOrders();

        Order cancelled = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void pointsRedeemAndHistory() throws Exception {
        User customer = userRepository.findByEmail("test@example.com").orElseThrow();
        long pointsBefore = customer.getPoints();

        addBookToCart(2);
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-points"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, 100L))))
                .andExpect(status().isCreated());

        User after = userRepository.findByEmail("test@example.com").orElseThrow();
        assertEquals(pointsBefore - 100, after.getPoints());

        mockMvc.perform(get("/api/auth/me/points")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(after.getPoints().intValue()));
    }

    @Test
    void idempotencyCheckoutReturnsExistingOrder() throws Exception {
        addBookToCart(1);
        String idempotencyKey = uniqueIdempotencyKey("checkout-idempotent");
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paymentUrl").exists());
    }

    @Test
    void lateWebhookBeforeCancellationRestoresPaidOrder() throws Exception {
        addBookToCart(1);
        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("late-webhook-restore"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = extractOrderId(checkout).longValue();
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        orderRepository.save(order);
        orderService.processExpiredPendingOrders();

        order = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        var result = new edu.fpt.sba301.bookstore.payment.WebhookResult(
                true,
                order.getId(),
                order.getTotal(),
                "MOCK-LATE-OK",
                "OK",
                order.getUpdatedAt().minusMinutes(1));

        var response = orderService.handlePaymentWebhook("mock", result);
        assertEquals(OrderStatus.PAID, response.status());
        assertFalse(response.manualRefundRequired());
    }

    @Test
    void lateWebhookAfterCancellationFlagsManualRefund() throws Exception {
        addBookToCart(1);
        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("late-webhook-refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = extractOrderId(checkout).longValue();
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setExpiresAt(OffsetDateTime.now().minusMinutes(10));
        order.setUpdatedAt(OffsetDateTime.now().minusMinutes(5));
        orderRepository.save(order);
        orderService.processExpiredPendingOrders();

        order = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        var result = new edu.fpt.sba301.bookstore.payment.WebhookResult(
                true,
                order.getId(),
                order.getTotal(),
                "MOCK-LATE-FAIL",
                "OK",
                OffsetDateTime.now());

        var response = orderService.handlePaymentWebhook("mock", result);
        assertEquals(OrderStatus.CANCELLED, response.status());
        assertTrue(response.manualRefundRequired());
    }

    @Test
    void rejectEmptyCartCheckout() throws Exception {
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-empty"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectCombiningPointsAndVoucher() throws Exception {
        addBookToCart(1);
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-both"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", "SAVE50K", 100L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot combine points and voucher."));
    }

    @Test
    void checkoutAppliesShippingFeeBelowFreeThreshold() throws Exception {
        Long atomicHabitsId = findBookIdByTitle("Atomic Habits");
        resetBookStock(atomicHabitsId, 100);
        addBookToCart(atomicHabitsId, 1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-shipping-fee"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal").value(280000))
                .andExpect(jsonPath("$.data.shippingFee").value(30000))
                .andExpect(jsonPath("$.data.total").value(310000));
    }

    @Test
    void checkoutFreeShippingAtOrAboveThreshold() throws Exception {
        addBookToCart(1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-free-ship"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal").value(350000))
                .andExpect(jsonPath("$.data.shippingFee").value(0))
                .andExpect(jsonPath("$.data.total").value(350000));
    }

    @Test
    void checkoutWithFixedVoucher() throws Exception {
        addBookToCart(1);
        int usedBefore = voucherRepository.findByCodeIgnoreCase("SAVE50K").orElseThrow().getUsedCount();

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-fixed-voucher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", "SAVE50K", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discount").value(50000))
                .andExpect(jsonPath("$.data.total").value(300000));

        int usedAfter = voucherRepository.findByCodeIgnoreCase("SAVE50K").orElseThrow().getUsedCount();
        assertEquals(usedBefore + 1, usedAfter);
    }

    @Test
    void checkoutWithShipVoucherWaivesShippingFee() throws Exception {
        Long atomicHabitsId = findBookIdByTitle("Atomic Habits");
        resetBookStock(atomicHabitsId, 100);
        addBookToCart(atomicHabitsId, 1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-ship-voucher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", "FREESHIP", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal").value(280000))
                .andExpect(jsonPath("$.data.shippingFee").value(0))
                .andExpect(jsonPath("$.data.total").value(280000));
    }

    @Test
    void checkoutWithPercentVoucher() throws Exception {
        addBookToCart(1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-percent-voucher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", "PERCENT10", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discount").value(35000))
                .andExpect(jsonPath("$.data.shippingFee").value(0))
                .andExpect(jsonPath("$.data.total").value(315000));
    }

    @Test
    void cancelPaidOrderRestoresStock() throws Exception {
        addBookToCart(1);
        Book book = bookRepository.findById(bookId).orElseThrow();
        int stockBeforeCheckout = book.getStock();
        int soldBefore = book.getSoldCount();

        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-cancel-paid"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Number orderId = extractOrderId(checkout);
        payOrderViaMockWebhook(checkout);

        Book afterPaid = bookRepository.findById(bookId).orElseThrow();
        assertEquals(soldBefore + 1, afterPaid.getSoldCount());

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        Book restored = bookRepository.findById(bookId).orElseThrow();
        assertEquals(stockBeforeCheckout, restored.getStock());
        assertEquals(soldBefore, restored.getSoldCount());
    }

    @Test
    void getAddressesForCheckout() throws Exception {
        mockMvc.perform(get("/api/auth/me/addresses")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(addressId.intValue()))
                .andExpect(jsonPath("$.data[0].city").value("Hanoi"));
    }

    @Test
    void lateWebhookRestoresPointsAfterTimeout() throws Exception {
        User customer = userRepository.findByEmail("test@example.com").orElseThrow();
        long pointsBefore = customer.getPoints();

        addBookToCart(1);
        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("late-webhook-points"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, 100L))))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = extractOrderId(checkout).longValue();
        assertEquals(pointsBefore - 100, userRepository.findByEmail("test@example.com").orElseThrow().getPoints());

        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        orderRepository.save(order);
        orderService.processExpiredPendingOrders();
        assertEquals(pointsBefore, userRepository.findByEmail("test@example.com").orElseThrow().getPoints());

        order = orderRepository.findById(orderId).orElseThrow();
        var result = new edu.fpt.sba301.bookstore.payment.WebhookResult(
                true,
                order.getId(),
                order.getTotal(),
                "MOCK-LATE-POINTS",
                "OK",
                order.getUpdatedAt().minusMinutes(1));
        var response = orderService.handlePaymentWebhook("mock", result);
        assertEquals(OrderStatus.PAID, response.status());
        assertEquals(pointsBefore - 100, userRepository.findByEmail("test@example.com").orElseThrow().getPoints());
    }

    @Test
    void rejectCancelShippedOrder() throws Exception {
        addBookToCart(1);
        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-no-cancel-shipped"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = extractOrderId(checkout).longValue();
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void voucherUsageReleasedWhenPendingOrderCancelled() throws Exception {
        addBookToCart(1);
        var voucher = voucherRepository.findByCodeIgnoreCase("SAVE50K").orElseThrow();
        int usedBefore = voucher.getUsedCount();

        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-voucher-release"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", "SAVE50K", null))))
                .andExpect(status().isCreated())
                .andReturn();
        assertEquals(usedBefore + 1, voucherRepository.findByCodeIgnoreCase("SAVE50K").orElseThrow().getUsedCount());

        Number orderId = extractOrderId(checkout);
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        assertEquals(usedBefore, voucherRepository.findByCodeIgnoreCase("SAVE50K").orElseThrow().getUsedCount());
    }

    @Test
    void creditOnDeliveredUpdatesLifetimePoints() {
        User customer = userRepository.findByEmail("test@example.com").orElseThrow();
        Order order = new Order();
        order.setUser(customer);
        order.setStatus(OrderStatus.DELIVERED);
        order.setSubtotal(500000L);
        order.setDiscount(0L);
        order.setShippingFee(0L);
        order.setTotal(500000L);
        order.setAddressSnapshot("{}");
        order.setPaymentMethod("mock");
        order.setPointsUsed(0L);
        order.setPointsEarned(0L);
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        order = orderRepository.save(order);

        pointService.creditOnDelivered(order);

        User refreshed = userRepository.findByEmail("test@example.com").orElseThrow();
        assertTrue(refreshed.getLifetimePoints() >= 50L);
    }

    @Test
    void orderDetailPreservesPriceAndVoucherSnapshots() throws Exception {
        addBookToCart(1);

        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-snapshot"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", "SAVE50K", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.voucherCode").value("SAVE50K"))
                .andExpect(jsonPath("$.data.items[0].titleSnapshot").value("Clean Code"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(350000))
                .andReturn();

        Number orderId = extractOrderId(checkout);

        Book book = bookRepository.findById(bookId).orElseThrow();
        book.setPrice(999999L);
        bookRepository.save(book);

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucherCode").value("SAVE50K"))
                .andExpect(jsonPath("$.data.items[0].titleSnapshot").value("Clean Code"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(350000))
                .andExpect(jsonPath("$.data.total").value(300000));
    }

    @Test
    void cancelPendingOrderWithPointsRestoresBalance() throws Exception {
        User customer = userRepository.findByEmail("test@example.com").orElseThrow();
        long pointsBefore = customer.getPoints();

        addBookToCart(1);
        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-cancel-points"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, 250L))))
                .andExpect(status().isCreated())
                .andReturn();
        Number orderId = extractOrderId(checkout);

        assertEquals(pointsBefore - 250, userRepository.findByEmail("test@example.com").orElseThrow().getPoints());

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertEquals(pointsBefore, userRepository.findByEmail("test@example.com").orElseThrow().getPoints());
    }

    @Test
    void checkoutMoneyMathInvariantWithVoucherAndShipping() throws Exception {
        addBookToCart(1);

        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", uniqueIdempotencyKey("checkout-money-math"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", "PERCENT10", null))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> data = (Map<?, ?>) jsonMapper.readValue(
                checkout.getResponse().getContentAsString(), Map.class).get("data");
        long subtotal = ((Number) data.get("subtotal")).longValue();
        long discount = ((Number) data.get("discount")).longValue();
        long shippingFee = ((Number) data.get("shippingFee")).longValue();
        long total = ((Number) data.get("total")).longValue();

        assertEquals(350000L, subtotal);
        assertEquals(35000L, discount);
        assertEquals(0L, shippingFee);
        assertEquals(subtotal - discount + shippingFee, total);
    }

    private String uniqueIdempotencyKey(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private Long findBookIdByTitle(String title) {
        return bookRepository.findAll().stream()
                .filter(book -> title.equals(book.getTitle()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void resetBookStock(Long targetBookId, int stock) {
        Book book = bookRepository.findById(targetBookId).orElseThrow();
        book.setStock(stock);
        bookRepository.save(book);
    }

    private void payOrderViaMockWebhook(MvcResult checkout) throws Exception {
        Map<?, ?> checkoutMap = jsonMapper.readValue(checkout.getResponse().getContentAsString(), Map.class);
        Map<?, ?> data = (Map<?, ?>) checkoutMap.get("data");
        String paymentUrl = (String) data.get("paymentUrl");
        assertNotNull(paymentUrl);
        String query = paymentUrl.substring(paymentUrl.indexOf('?') + 1);
        mockMvc.perform(get("/api/payment/webhook/mock?" + query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    private void addBookToCart(int qty) throws Exception {
        addBookToCart(bookId, qty);
    }

    private void addBookToCart(Long targetBookId, int qty) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CartItemRequest(targetBookId, qty))))
                .andExpect(status().isOk());
    }

    private String login(String email, String password, List<GuestCartItemRequest> guestItems) throws Exception {
        LoginRequest request = guestItems == null
                ? new LoginRequest(email, password)
                : new LoginRequest(email, password, guestItems);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> map = jsonMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<?, ?> data = (Map<?, ?>) map.get("data");
        return (String) data.get("accessToken");
    }

    private Number extractOrderId(MvcResult result) throws Exception {
        Map<?, ?> map = jsonMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<?, ?> data = (Map<?, ?>) map.get("data");
        return (Number) data.get("id");
    }
}
