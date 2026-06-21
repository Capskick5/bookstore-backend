package edu.fpt.sba301.bookstore;

import tools.jackson.databind.json.JsonMapper;
import edu.fpt.sba301.bookstore.dto.request.ReviewRequest;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.OrderItem;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.enums.OrderStatus;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.OrderItemRepository;
import edu.fpt.sba301.bookstore.repository.OrderRepository;
import edu.fpt.sba301.bookstore.repository.ReviewRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Long bookId;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        Book book = bookRepository.findAll().stream()
                .filter(b -> "Clean Code".equals(b.getTitle()))
                .findFirst()
                .orElseThrow();
        bookId = book.getId();

        User customer = userRepository.findByEmail("test@example.com").orElseThrow();
        reviewRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(customer.getId()) && r.getBook().getId().equals(bookId))
                .forEach(reviewRepository::delete);

        seedDeliveredPurchase(customer, book);

        customerToken = login("test@example.com", "password123");
        adminToken = login("admin@example.com", "adminpassword123");
    }

    @Test
    void listReviewsIsPublic() throws Exception {
        mockMvc.perform(get("/api/books/" + bookId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createReviewUpdatesRatingAvg() throws Exception {
        mockMvc.perform(post("/api/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ReviewRequest(5, "Great book!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.reviewerName").exists());

        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ratingAvg").value(5.0));
    }

    @Test
    void rejectDuplicateReview() throws Exception {
        mockMvc.perform(post("/api/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ReviewRequest(4, "First review"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ReviewRequest(3, "Duplicate"))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectReviewWithoutDeliveredPurchase() throws Exception {
        Book freshBook = new Book();
        Book template = bookRepository.findById(bookId).orElseThrow();
        freshBook.setTitle("Stress Review Book " + System.nanoTime());
        freshBook.setAuthor("Test Author");
        freshBook.setCategory(template.getCategory());
        freshBook.setPrice(100_000L);
        freshBook.setStock(10);
        freshBook.setActive(true);
        freshBook.setRatingAvg(java.math.BigDecimal.ZERO);
        freshBook.setSoldCount(0);
        freshBook.setCreatedAt(OffsetDateTime.now());
        freshBook.setUpdatedAt(OffsetDateTime.now());
        freshBook = bookRepository.save(freshBook);

        mockMvc.perform(post("/api/books/" + freshBook.getId() + "/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ReviewRequest(5, "No purchase"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOwnReview() throws Exception {
        var result = mockMvc.perform(post("/api/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ReviewRequest(4, "Will delete"))))
                .andExpect(status().isCreated())
                .andReturn();

        var map = jsonMapper.readValue(result.getResponse().getContentAsString(), java.util.Map.class);
        Number reviewId = (Number) ((java.util.Map<?, ?>) map.get("data")).get("id");

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanDeleteAnyReview() throws Exception {
        var result = mockMvc.perform(post("/api/books/" + bookId + "/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ReviewRequest(3, "Admin delete"))))
                .andExpect(status().isCreated())
                .andReturn();

        var map = jsonMapper.readValue(result.getResponse().getContentAsString(), java.util.Map.class);
        Number reviewId = (Number) ((java.util.Map<?, ?>) map.get("data")).get("id");

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private void seedDeliveredPurchase(User customer, Book book) {
        Order order = new Order();
        order.setUser(customer);
        order.setStatus(OrderStatus.DELIVERED);
        order.setSubtotal(book.getPrice());
        order.setDiscount(0L);
        order.setShippingFee(0L);
        order.setTotal(book.getPrice());
        order.setAddressSnapshot("{}");
        order.setPaymentMethod("cod");
        order.setPointsUsed(0L);
        order.setPointsEarned(0L);
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        order.setManualRefundRequired(false);
        order = orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setBook(book);
        item.setTitleSnapshot(book.getTitle());
        item.setUnitPrice(book.getPrice());
        item.setQuantity(1);
        orderItemRepository.save(item);
    }

    private String login(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new edu.fpt.sba301.bookstore.dto.request.LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        var map = jsonMapper.readValue(result.getResponse().getContentAsString(), java.util.Map.class);
        return (String) ((java.util.Map<?, ?>) map.get("data")).get("accessToken");
    }
}
