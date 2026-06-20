package edu.fpt.sba301.bookstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fpt.sba301.bookstore.dto.request.BookRequest;
import edu.fpt.sba301.bookstore.dto.request.CategoryRequest;
import edu.fpt.sba301.bookstore.dto.request.LoginRequest;
import edu.fpt.sba301.bookstore.dto.request.RegisterRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Category;
import edu.fpt.sba301.bookstore.entity.Order;
import edu.fpt.sba301.bookstore.entity.OrderItem;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.CategoryRepository;
import edu.fpt.sba301.bookstore.repository.OrderItemRepository;
import edu.fpt.sba301.bookstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCatalogTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private edu.fpt.sba301.bookstore.repository.UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Get Admin token
        LoginRequest adminLogin = new LoginRequest("admin@example.com", "adminpassword123");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = extractToken(adminResult);

        // Register a unique customer so this test does not depend on local seed history.
        String customerEmail = "catalog-test-" + java.util.UUID.randomUUID() + "@example.com";
        String customerPassword = "password123";
        RegisterRequest registerRequest = new RegisterRequest(customerEmail, customerPassword, "Catalog Test User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest customerLogin = new LoginRequest(customerEmail, customerPassword);
        MvcResult customerResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerLogin)))
                .andExpect(status().isOk())
                .andReturn();
        customerToken = extractToken(customerResult);
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(body, Map.class);
        Map<?, ?> dataMap = (Map<?, ?>) map.get("data");
        return (String) dataMap.get("accessToken");
    }

    @Test
    void testAccessControl() throws Exception {
        // Customer trying to access Admin Category CRUD should get 403
        mockMvc.perform(get("/api/admin/categories")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Anonymous trying to access Admin Category CRUD should get 401
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCategoryCRUD() throws Exception {
        String uniqueName = "Test Category " + java.util.UUID.randomUUID();

        // 1. Create category
        CategoryRequest createReq = new CategoryRequest(uniqueName);
        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(uniqueName))
                .andExpect(jsonPath("$.data.slug").exists())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(body, Map.class);
        Map<?, ?> data = (Map<?, ?>) map.get("data");
        Number categoryId = (Number) data.get("id");

        // 2. Create duplicate name -> 409
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict());

        // 3. Update category
        String updatedName = uniqueName + " Updated";
        CategoryRequest updateReq = new CategoryRequest(updatedName);
        mockMvc.perform(put("/api/admin/categories/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(updatedName));

        // 4. Delete category
        mockMvc.perform(delete("/api/admin/categories/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify deleted
        assertFalse(categoryRepository.findById(categoryId.longValue()).isPresent());
    }

    @Test
    void testCategoryDeleteBlocked() throws Exception {
        // Create a category
        Category category = new Category();
        category.setName("Blocked Cat " + java.util.UUID.randomUUID());
        category.setSlug("blocked-cat-" + java.util.UUID.randomUUID());
        category = categoryRepository.save(category);

        // Create a book referencing this category
        Book book = new Book();
        book.setTitle("Ref Book");
        book.setAuthor("Author");
        book.setCategory(category);
        book.setPrice(100000L);
        book.setStock(10);
        book.setRatingAvg(BigDecimal.ZERO);
        book.setSoldCount(0);
        book.setActive(true);
        book.setCreatedAt(OffsetDateTime.now());
        book.setUpdatedAt(OffsetDateTime.now());
        book = bookRepository.save(book);

        // Attempt to delete category -> 409 Conflict
        mockMvc.perform(delete("/api/admin/categories/" + category.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // Clean up
        bookRepository.delete(book);
        categoryRepository.delete(category);
    }

    @Test
    void testBookCRUD() throws Exception {
        // Seed category for book
        Category category = new Category();
        category.setName("Book Cat " + java.util.UUID.randomUUID());
        category.setSlug("book-cat-" + java.util.UUID.randomUUID());
        category = categoryRepository.save(category);

        // 1. Create Book
        BookRequest createReq = new BookRequest(
                "Test Title", "Test Author", category.getId(),
                150000L, 200000L, 50, "Description", "http://cover.url", true
        );

        MvcResult createResult = mockMvc.perform(post("/api/admin/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Test Title"))
                .andExpect(jsonPath("$.data.price").value(150000))
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(body, Map.class);
        Map<?, ?> data = (Map<?, ?>) map.get("data");
        Number bookId = (Number) data.get("id");

        // The newly created active book must be visible through the public catalog.
        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Title"));

        mockMvc.perform(get("/api/books").param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(50))
                .andExpect(jsonPath("$.data.totalElements").isNumber())
                .andExpect(jsonPath("$.data.totalPages").isNumber());

        // 2. Validate price=0 and stock=0 -> 400
        BookRequest invalidReq = new BookRequest(
                "Test Title", "Test Author", category.getId(),
                0L, 0L, 0, "Description", "http://cover.url", true
        );
        mockMvc.perform(post("/api/admin/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());

        // 3. Update Book
        BookRequest updateReq = new BookRequest(
                "Updated Title", "Test Author", category.getId(),
                180000L, 250000L, 40, "Updated Desc", "http://cover.url", true
        );
        mockMvc.perform(put("/api/admin/books/" + bookId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"));

        BookRequest hideReq = new BookRequest(
                "Updated Title", "Test Author", category.getId(),
                180000L, 250000L, 40, "Updated Desc", "http://cover.url", false
        );
        mockMvc.perform(put("/api/admin/books/" + bookId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hideReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isNotFound());

        // 4. Hard Delete Book (not referenced by orders)
        mockMvc.perform(delete("/api/admin/books/" + bookId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify deleted
        assertFalse(bookRepository.findById(bookId.longValue()).isPresent());

        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isNotFound());

        // Clean up
        categoryRepository.delete(category);
    }

    @Test
    void testBookSoftDelete() throws Exception {
        // Seed category for book
        Category category = new Category();
        category.setName("Soft Delete Cat " + java.util.UUID.randomUUID());
        category.setSlug("soft-delete-cat-" + java.util.UUID.randomUUID());
        category = categoryRepository.save(category);

        // Seed Book
        Book book = new Book();
        book.setTitle("Order Book");
        book.setAuthor("Author");
        book.setCategory(category);
        book.setPrice(100000L);
        book.setStock(10);
        book.setRatingAvg(BigDecimal.ZERO);
        book.setSoldCount(0);
        book.setActive(true);
        book.setCreatedAt(OffsetDateTime.now());
        book.setUpdatedAt(OffsetDateTime.now());
        book = bookRepository.save(book);

        // Fetch user for order creation
        var admin = userRepository.findByEmail("admin@example.com")
                .orElseThrow(() -> new IllegalStateException("Admin user not found"));

        // Create a dummy order
        Order order = new Order();
        order.setUser(admin);
        order.setStatus("PENDING");
        order.setSubtotal(100000L);
        order.setDiscount(0L);
        order.setShippingFee(30000L);
        order.setTotal(130000L);
        order.setAddressSnapshot("123 Main St");
        order.setPaymentMethod("COD");
        order.setPointsUsed(0L);
        order.setPointsEarned(0L);
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        order = orderRepository.save(order);

        // Create order item referencing the book
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setBook(book);
        orderItem.setTitleSnapshot(book.getTitle());
        orderItem.setUnitPrice(book.getPrice());
        orderItem.setQuantity(1);
        orderItem = orderItemRepository.save(orderItem);

        // Delete book -> should trigger soft delete
        mockMvc.perform(delete("/api/admin/books/" + book.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify soft-deleted
        Book deletedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(deletedBook);
        assertFalse(deletedBook.getActive());

        // Clean up
        orderItemRepository.delete(orderItem);
        orderRepository.delete(order);
        bookRepository.delete(deletedBook);
        categoryRepository.delete(category);
    }
}
