package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.AdminStatsResponse;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

import static edu.fpt.sba301.bookstore.config.SwaggerConfig.BEARER_AUTH;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Stats", description = "Dashboard statistics")
@SecurityRequirement(name = BEARER_AUTH)
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "Get dashboard statistics")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AdminStatsResponse data = adminStatsService.getStats(startDate, endDate);

        ApiResponse<AdminStatsResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }
}
