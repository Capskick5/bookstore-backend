package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.response.AdminStatsResponse;

import java.time.LocalDate;

public interface AdminStatsService {
    AdminStatsResponse getStats(LocalDate startDate, LocalDate endDate);
}
