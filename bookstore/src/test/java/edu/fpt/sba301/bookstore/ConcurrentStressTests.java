package edu.fpt.sba301.bookstore;

import edu.fpt.sba301.bookstore.dto.request.CheckoutRequest;
import edu.fpt.sba301.bookstore.entity.Address;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.entity.Voucher;
import edu.fpt.sba301.bookstore.enums.OrderStatus;
import edu.fpt.sba301.bookstore.enums.Role;
import edu.fpt.sba301.bookstore.repository.AddressRepository;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.OrderRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.repository.VoucherRedemptionRepository;
import edu.fpt.sba301.bookstore.repository.VoucherRepository;
import edu.fpt.sba301.bookstore.service.CartService;
import edu.fpt.sba301.bookstore.service.CheckoutResult;
import edu.fpt.sba301.bookstore.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WS3 stress tests — verify the cart + checkout + voucher paths under concurrent load.
 * <p>Each scenario fires N parallel threads from a shared start latch so contention is real
 * (one Postgres transaction per checkout, no shared @Transactional from the test thread).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentStressTests {

    private static final String EMAIL_PREFIX = "stress-";
    private static final int AWAIT_TIMEOUT_SECONDS = 60;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherRedemptionRepository voucherRedemptionRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long bookId;
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdVoucherIds = new ArrayList<>();

    @BeforeEach
    void resetBookStock() {
        Book book = bookRepository.findAll().stream()
                .filter(b -> "Clean Code".equals(b.getTitle()))
                .findFirst()
                .orElseThrow();
        bookId = book.getId();
        book.setPrice(350_000L);
        book.setOriginalPrice(400_000L);
        book.setStock(50);
        book.setActive(true);
        bookRepository.save(book);
    }

    @AfterEach
    void cleanupStressArtifacts() {
        cancelPendingOrdersForCreatedUsers();
        bookRepository.findById(bookId).ifPresent(book -> {
            book.setStock(50);
            bookRepository.save(book);
        });
        for (Long voucherId : createdVoucherIds) {
            voucherRepository.findById(voucherId).ifPresent(voucher -> {
                voucherRedemptionRepository.findAll().stream()
                        .filter(redemption -> redemption.getVoucher().getId().equals(voucherId))
                        .forEach(voucherRedemptionRepository::delete);
                voucherRepository.delete(voucher);
            });
        }
        createdVoucherIds.clear();
        createdUserIds.clear();
    }

    @Test
    @DisplayName("Oversell prevention — 20 concurrent buyers vs stock=5 produces exactly 5 winners")
    void oversellPreventionUnderConcurrency() throws Exception {
        int stock = 5;
        int contenders = 20;
        setBookStock(bookId, stock);

        List<User> users = createUsersWithSingleItemCart(contenders, bookId, 1);
        ConcurrencyResult result = runConcurrentCheckouts(users, null);

        assertEquals(stock, result.successCount.get(),
                "Exactly " + stock + " checkouts should succeed when stock=" + stock);
        assertEquals(contenders - stock, result.failureCount.get(),
                "Remaining buyers must be rejected without overselling");

        int finalStock = bookRepository.findById(bookId).orElseThrow().getStock();
        assertEquals(0, finalStock, "Final stock must be zero, never negative");

        long pendingOrders = orderRepository.findAll().stream()
                .filter(o -> createdUserIds.contains(o.getUser().getId()))
                .filter(o -> OrderStatus.PENDING.equals(o.getStatus()))
                .count();
        assertEquals(stock, pendingOrders, "One PENDING order per successful checkout");
    }

    @Test
    @DisplayName("Voucher usage_limit — 10 concurrent buyers vs limit=3 admits exactly 3")
    void voucherUsageLimitUnderConcurrency() throws Exception {
        int contenders = 10;
        int usageLimit = 3;
        setBookStock(bookId, contenders);

        Voucher voucher = createLimitedVoucher(usageLimit);
        List<User> users = createUsersWithSingleItemCart(contenders, bookId, 1);

        ConcurrencyResult result = runConcurrentCheckouts(users, voucher.getCode());

        assertEquals(usageLimit, result.successCount.get(),
                "Exactly " + usageLimit + " buyers should redeem the voucher");
        assertEquals(contenders - usageLimit, result.failureCount.get(),
                "Remaining buyers must be rejected once usage_limit is reached");

        Voucher refreshed = voucherRepository.findById(voucher.getId()).orElseThrow();
        assertEquals(usageLimit, refreshed.getUsedCount(),
                "Voucher used_count must equal usage_limit; never exceed");
        assertTrue(refreshed.getUsedCount() <= refreshed.getUsageLimit(),
                "used_count must never exceed usage_limit under any race");
    }

    @Test
    @DisplayName("Idempotency-Key — 8 concurrent checkouts with same key create exactly one order")
    void idempotencyKeyDeduplicatesConcurrentCheckouts() throws Exception {
        int duplicates = 8;
        setBookStock(bookId, 50);

        User singleUser = createUserWithSingleItemCart(0, bookId, 1);
        String sharedKey = "stress-idempotent-" + UUID.randomUUID();
        Long stockBefore = (long) bookRepository.findById(bookId).orElseThrow().getStock();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        List<Long> returnedOrderIds = java.util.Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(duplicates);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(duplicates);

        for (int i = 0; i < duplicates; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    CheckoutRequest req = new CheckoutRequest(addressIdOf(singleUser), "cod", null, null);
                    CheckoutResult cr = orderService.checkout(singleUser, req, sharedKey, "http://localhost");
                    returnedOrderIds.add(cr.order().id());
                    successCount.incrementAndGet();
                } catch (Throwable ex) {
                    failureCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Idempotency stress run timed out");
        executor.shutdownNow();

        long distinctOrderIds = returnedOrderIds.stream().distinct().count();
        assertEquals(1L, distinctOrderIds,
                "All successful idempotent calls must point to the SAME order id");

        assertTrue(orderRepository.findByIdempotencyKey(sharedKey).isPresent(),
                "Database must hold exactly one order with the shared key");

        int stockAfter = bookRepository.findById(bookId).orElseThrow().getStock();
        assertEquals(stockBefore - 1, stockAfter,
                "Stock must decrease by exactly 1 even with " + duplicates + " concurrent calls");
        assertTrue(successCount.get() >= 1, "At least one thread must observe the idempotent order");
    }

    private ConcurrencyResult runConcurrentCheckouts(List<User> users, String voucherCode) throws Exception {
        int n = users.size();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(n, 16));
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (User user : users) {
            executor.submit(() -> {
                try {
                    start.await();
                    CheckoutRequest req = new CheckoutRequest(
                            addressIdOf(user), "cod", voucherCode, null);
                    String key = "stress-" + user.getId() + "-" + UUID.randomUUID();
                    orderService.checkout(user, req, key, "http://localhost");
                    successCount.incrementAndGet();
                } catch (Throwable ex) {
                    failureCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Stress run timed out");
        executor.shutdownNow();
        return new ConcurrencyResult(successCount, failureCount);
    }

    private List<User> createUsersWithSingleItemCart(int count, Long targetBookId, int quantity) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createUserWithSingleItemCart(i, targetBookId, quantity));
        }
        return users;
    }

    private User createUserWithSingleItemCart(int index, Long targetBookId, int quantity) {
        String email = EMAIL_PREFIX + UUID.randomUUID() + "-" + index + "@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("stress-pass"));
        user.setFullName("Stress User " + index);
        user.setRole(Role.CUSTOMER.name());
        user.setEnabled(true);
        user.setPoints(0L);
        user.setLifetimePoints(0L);
        user.setTier("SILVER");
        user.setCreatedAt(OffsetDateTime.now());
        user = userRepository.save(user);

        Address address = new Address();
        address.setUser(user);
        address.setRecipient("Stress " + index);
        address.setPhone("0900000000");
        address.setLine("Stress lane " + index);
        address.setCity("Hanoi");
        address.setIsDefault(true);
        addressRepository.save(address);

        cartService.addItem(user, targetBookId, quantity);
        createdUserIds.add(user.getId());
        return user;
    }

    private Long addressIdOf(User user) {
        return addressRepository.findAllByUserId(user.getId()).getFirst().getId();
    }

    private Voucher createLimitedVoucher(int usageLimit) {
        Voucher voucher = new Voucher();
        voucher.setCode("STRESS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        voucher.setType("FIXED");
        voucher.setValue(10_000L);
        voucher.setMinOrder(0L);
        voucher.setMaxDiscount(null);
        voucher.setUsageLimit(usageLimit);
        voucher.setPerUserLimit(1);
        voucher.setStartsAt(OffsetDateTime.now().minusDays(1));
        voucher.setEndsAt(OffsetDateTime.now().plusDays(1));
        voucher.setActive(true);
        voucher.setUsedCount(0);
        Voucher saved = voucherRepository.save(voucher);
        createdVoucherIds.add(saved.getId());
        return saved;
    }

    private void setBookStock(Long targetBookId, int stock) {
        Book book = bookRepository.findById(targetBookId).orElseThrow();
        book.setStock(stock);
        bookRepository.save(book);
    }

    private void cancelPendingOrdersForCreatedUsers() {
        for (Long uid : createdUserIds) {
            userRepository.findById(uid).ifPresent(user ->
                    orderRepository.findByUserOrderByCreatedAtDesc(user, Pageable.unpaged()).stream()
                            .filter(order -> OrderStatus.PENDING.equals(order.getStatus()))
                            .forEach(order -> orderService.cancelOrder(user, order.getId()))
            );
        }
    }

    private record ConcurrencyResult(AtomicInteger successCount, AtomicInteger failureCount) {
    }
}
