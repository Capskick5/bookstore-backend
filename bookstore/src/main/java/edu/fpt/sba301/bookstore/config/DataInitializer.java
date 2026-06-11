package edu.fpt.sba301.bookstore.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DataInitializer implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {

    }
}
