package edu.fpt.sba301.bookstore.scheduler;

import edu.fpt.sba301.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.order.timeout-check-ms:60000}")
    public void cancelExpiredOrders() {
        try {
            orderService.processExpiredPendingOrders();
        } catch (Exception e) {
            log.error("Failed to process expired pending orders", e);
        }
    }
}
