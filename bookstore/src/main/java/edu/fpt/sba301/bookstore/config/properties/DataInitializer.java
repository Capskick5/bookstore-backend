package edu.fpt.sba301.bookstore.config.properties;

import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.enums.Role;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // test customer user
        String customerEmail = "test@example.com";
        String customerPassword = "password123";

        if (userRepository.findByEmail(customerEmail).isEmpty()) {
            User user = new User();
            user.setEmail(customerEmail);
            user.setPasswordHash(passwordEncoder.encode(customerPassword));
            user.setFullName("Test User");
            user.setRole(Role.CUSTOMER.name());
            user.setEnabled(true);
            user.setPoints(0L);
            user.setLifetimePoints(0L);
            user.setTier("SILVER");
            user.setCreatedAt(OffsetDateTime.now());

            userRepository.save(user);
            System.out.println("Created test customer user: " + customerEmail + " / " + customerPassword);
        }

        // test admin user
        String adminEmail = "admin@example.com";
        String adminPassword = "adminpassword123";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setFullName("Admin User");
            admin.setRole(Role.ADMIN.name());
            admin.setEnabled(true);
            admin.setPoints(0L);
            admin.setLifetimePoints(0L);
            admin.setTier("SILVER");
            admin.setCreatedAt(OffsetDateTime.now());

            userRepository.save(admin);
            System.out.println("Created test admin user: " + adminEmail + " / " + adminPassword);
        }

        seedCatalog();
    }

    private void seedCatalog() {
        seedCategory("Programming", "programming");
        seedCategory("Business", "business");
        seedCategory("Self Development", "self-development");
        seedCategory("Fiction", "fiction");

        seedBook("Clean Code", "Robert C. Martin", "programming", 320000L, 390000L, 20,
                "A practical guide to writing readable, maintainable, and professional software.");
        seedBook("Effective Java", "Joshua Bloch", "programming", 450000L, 520000L, 15,
                "Best practices for building robust Java applications and APIs.");
        seedBook("Designing Data-Intensive Applications", "Martin Kleppmann", "programming", 620000L, 700000L, 10,
                "A deep look at data systems, consistency, storage, distributed processing, and reliability.");
        seedBook("Spring in Action", "Craig Walls", "programming", 410000L, 480000L, 18,
                "A hands-on guide to building modern Spring applications.");
        seedBook("The Lean Startup", "Eric Ries", "business", 260000L, 320000L, 25,
                "A startup methodology focused on validated learning, experimentation, and iterative products.");
        seedBook("Zero to One", "Peter Thiel", "business", 240000L, 300000L, 22,
                "A book about building original companies and thinking about monopoly, innovation, and progress.");
        seedBook("Atomic Habits", "James Clear", "self-development", 210000L, 260000L, 30,
                "A practical system for building good habits and breaking bad ones through small improvements.");
        seedBook("Deep Work", "Cal Newport", "self-development", 230000L, 290000L, 16,
                "A guide to focused work, attention management, and producing high-value results.");
        seedBook("The Pragmatic Programmer", "David Thomas and Andrew Hunt", "programming", 360000L, 420000L, 14,
                "Timeless lessons on software craftsmanship, debugging, design, and professional development.");
        seedBook("Rich Dad Poor Dad", "Robert T. Kiyosaki", "business", 190000L, 240000L, 28,
                "A personal finance book contrasting different mindsets about assets, money, and work.");
        seedBook("Norwegian Wood", "Haruki Murakami", "fiction", 180000L, 230000L, 12,
                "A literary novel about memory, love, loss, and coming of age.");
        seedBook("Dune", "Frank Herbert", "fiction", 350000L, 420000L, 9,
                "A science fiction epic about politics, ecology, religion, and power on the desert planet Arrakis.");
    }

    private void seedCategory(String name, String slug) {
        jdbcTemplate.update(
                "INSERT INTO categories(name, slug) VALUES (?, ?) ON CONFLICT (slug) DO NOTHING",
                name,
                slug
        );
    }

    private void seedBook(String title, String author, String categorySlug, Long price, Long originalPrice,
                          Integer stock, String description) {
        jdbcTemplate.update("""
                INSERT INTO books(title, author, category_id, price, original_price, stock, description, cover_url)
                SELECT ?, ?, c.id, ?, ?, ?, ?, ?
                FROM categories c
                WHERE c.slug = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM books b WHERE b.title = ? AND b.author = ?
                  )
                """,
                title,
                author,
                price,
                originalPrice,
                stock,
                description,
                "https://placehold.co/320x480?text=" + title.replace(" ", "+"),
                categorySlug,
                title,
                author
        );
    }
}
