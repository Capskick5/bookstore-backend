package edu.fpt.sba301.bookstore.dto.response;

import java.util.List;

public record AdminStatsResponse(
        long totalBooks,
        long totalCategories,
        long totalUsers,
        long totalOrders,
        long totalRevenue,
        List<TopBookStatResponse> topBooks
) {
}
