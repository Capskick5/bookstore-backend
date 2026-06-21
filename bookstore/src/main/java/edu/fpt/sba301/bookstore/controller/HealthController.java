package edu.fpt.sba301.bookstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Service health checks")
public class HealthController {

    private final DataSource dataSource;

    @Operation(summary = "Check application and database health")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "bookstore");
        body.put("timestamp", OffsetDateTime.now());

        // optimistic status
        String overallStatus = "UP";

        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            if (rs.next()) {
                DatabaseMetaData md = conn.getMetaData();
                db.put("status", "UP");
                db.put("productName", md.getDatabaseProductName());
                db.put("productVersion", md.getDatabaseProductVersion());
            } else {
                db.put("status", "UNKNOWN");
                overallStatus = "DOWN";
            }
        } catch (Exception ex) {
            db.put("status", "DOWN");
            db.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            overallStatus = "DOWN";
        }

        body.put("status", overallStatus);
        body.put("db", db);

        HttpStatus httpStatus = "UP".equals(overallStatus) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(body);
    }
}