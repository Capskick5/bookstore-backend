package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.response.AdminStatsResponse;
import edu.fpt.sba301.bookstore.dto.response.TopBookStatResponse;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.CategoryRepository;
import edu.fpt.sba301.bookstore.repository.OrderItemRepository;
import edu.fpt.sba301.bookstore.repository.OrderRepository;
import edu.fpt.sba301.bookstore.repository.TopBookSalesProjection;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import edu.fpt.sba301.bookstore.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);
        OffsetDateTime start = range.start().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endExclusive = range.end().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        long totalRevenue = orderRepository.sumRevenueBetween(start, endExclusive);
        long totalOrders = orderRepository.countNonCancelledBetween(start, endExclusive);
        List<TopBookStatResponse> topBooks = orderItemRepository.findTopSellingBooks(start, endExclusive).stream()
                .map(this::mapTopBook)
                .toList();

        return new AdminStatsResponse(
                bookRepository.count(),
                categoryRepository.count(),
                userRepository.count(),
                totalOrders,
                totalRevenue,
                topBooks);
    }

    private TopBookStatResponse mapTopBook(TopBookSalesProjection row) {
        return new TopBookStatResponse(row.getBookId(), row.getTitle(), row.getSoldCount());
    }

    private DateRange resolveRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = startDate != null ? startDate : end.minusDays(DEFAULT_RANGE_DAYS);

        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be on or before endDate");
        }
        return new DateRange(start, end);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
