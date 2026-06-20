package edu.fpt.sba301.bookstore;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import edu.fpt.sba301.bookstore.service.OrderService;
import edu.fpt.sba301.bookstore.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
    private ObjectMapper objectMapper;

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
    private OrderService orderService;

    @Autowired
    private PointService pointService;

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

        User customer = userRepository.findByEmail("test@example.com").orElseThrow();
        addressId = addressRepository.findAllByUserId(customer.getId()).getFirst().getId();

        cartRepository.findByUser(customer).ifPresent(cart -> cartItemRepository.deleteByCart(cart));

        customerToken = login("test@example.com", "password123", null);
    }

    @Test
    void cartCrudAndSubtotal() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemRequest(bookId, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(700000))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(350000));

        mockMvc.perform(put("/api/cart/items/" + bookId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemRequest(bookId, 1))))
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
                        .content(objectMapper.writeValueAsString(new CartItemRequest(bookId, tooMany))))
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
                        .header("Idempotency-Key", "checkout-test-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
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
                        .header("Idempotency-Key", "checkout-webhook-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> checkoutMap = objectMapper.readValue(checkout.getResponse().getContentAsString(), Map.class);
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
                        .header("Idempotency-Key", "checkout-webhook-fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> failMap = objectMapper.readValue(failCheckout.getResponse().getContentAsString(), Map.class);
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
                        .header("Idempotency-Key", "checkout-history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
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
        int stockAfterReservation = book.getStock();

        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "checkout-cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CheckoutRequest(addressId, "mock", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        Number orderId = extractOrderId(checkout);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        Book restored = bookRepository.findById(bookId).orElseThrow();
        assertEquals(stockAfterReservation + 1, restored.getStock());
    }

    @Test
    void expiredPendingOrderIsCancelledByScheduler() throws Exception {
        addBookToCart(1);
        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "checkout-timeout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
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
                        .header("Idempotency-Key", "checkout-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
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

    private void addBookToCart(int qty) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemRequest(bookId, qty))))
                .andExpect(status().isOk());
    }

    private String login(String email, String password, List<GuestCartItemRequest> guestItems) throws Exception {
        LoginRequest request = guestItems == null
                ? new LoginRequest(email, password)
                : new LoginRequest(email, password, guestItems);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<?, ?> data = (Map<?, ?>) map.get("data");
        return (String) data.get("accessToken");
    }

    private Number extractOrderId(MvcResult result) throws Exception {
        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<?, ?> data = (Map<?, ?>) map.get("data");
        return (Number) data.get("id");
    }
}
