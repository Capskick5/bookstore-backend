package edu.fpt.sba301.bookstore;

import tools.jackson.databind.json.JsonMapper;
import edu.fpt.sba301.bookstore.constant.LoyaltyConstants;
import edu.fpt.sba301.bookstore.dto.request.CartItemRequest;
import edu.fpt.sba301.bookstore.dto.request.CheckoutRequest;
import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.request.UpdateOrderStatusRequest;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.enums.OrderStatus;
import edu.fpt.sba301.bookstore.repository.AddressRepository;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.OrderRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.service.CartService;
import edu.fpt.sba301.bookstore.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminOrderTests {

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
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    private String adminToken;
    private String customerToken;
    private Long bookId;
    private Long addressId;

    @BeforeEach
    void setUp() throws Exception {
        bookId = bookRepository.findAll().stream()
                .filter(b -> "Atomic Habits".equals(b.getTitle()))
                .findFirst()
                .orElseThrow()
                .getId();

        var customer = userRepository.findByEmail("test@example.com").orElseThrow();
        customer.setEnabled(true);
        userRepository.save(customer);
        addressId = addressRepository.findAllByUserId(customer.getId()).getFirst().getId();
        cartService.clearCart(customer);

        adminToken = login("admin@example.com", "adminpassword123");
        customerToken = login("test@example.com", "password123");
    }

    @Test
    void codCheckoutAndAdminConfirmFlow() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CartItemRequest(bookId, 1))))
                .andExpect(status().isOk());

        CheckoutRequest checkout = new CheckoutRequest(addressId, "cod", null, null);
        MvcResult checkoutResult = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "cod-flow-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(checkout)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value(OrderStatus.PENDING))
                .andExpect(jsonPath("$.data.paymentMethod").value("cod"))
                .andExpect(jsonPath("$.data.paymentUrl").doesNotExist())
                .andReturn();

        Number orderId = extractOrderId(checkoutResult);

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", OrderStatus.PENDING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(orderId.intValue()));

        mockMvc.perform(post("/api/admin/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(OrderStatus.PAID));

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.SHIPPED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(OrderStatus.SHIPPED));

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(OrderStatus.DELIVERED));

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(OrderStatus.DELIVERED));
    }

    @Test
    void adminRefundCancelAfterDeliveredReversesPointsAndRestoresStock() throws Exception {
        User customer = userRepository.findByEmail("test@example.com").orElseThrow();
        long pointsBefore = customer.getPoints();
        Book book = bookRepository.findById(bookId).orElseThrow();
        int stockBefore = book.getStock();
        int soldBefore = book.getSoldCount();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CartItemRequest(bookId, 1))))
                .andExpect(status().isOk());

        CheckoutRequest checkout = new CheckoutRequest(addressId, "cod", null, null);
        MvcResult checkoutResult = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "refund-flow-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(checkout)))
                .andExpect(status().isCreated())
                .andReturn();
        Number orderId = extractOrderId(checkoutResult);

        mockMvc.perform(post("/api/admin/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.SHIPPED))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                .andExpect(status().isOk());

        User afterDelivered = userRepository.findByEmail("test@example.com").orElseThrow();
        Order deliveredOrder = orderRepository.findById(orderId.longValue()).orElseThrow();
        long expectedEarned = deliveredOrder.getTotal() / LoyaltyConstants.POINTS_EARNED_VND_DIVISOR;
        assertEquals(pointsBefore + expectedEarned, afterDelivered.getPoints());

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(OrderStatus.CANCELLED));

        Book restored = bookRepository.findById(bookId).orElseThrow();
        assertEquals(stockBefore, restored.getStock());
        assertEquals(soldBefore, restored.getSoldCount());

        User afterCancel = userRepository.findByEmail("test@example.com").orElseThrow();
        assertEquals(pointsBefore, afterCancel.getPoints());
    }

    @Test
    void customerCannotAccessAdminOrders() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new LoginRequest(email, password))))
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
