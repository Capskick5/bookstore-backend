package edu.fpt.sba301.bookstore.event;

import edu.fpt.sba301.bookstore.service.RagReindexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookChangedEventListener {

    private final RagReindexService ragReindexService;

    @Async
    @EventListener
    public void handleBookChangedEvent(BookChangedEvent event) {
        log.info("Received BookChangedEvent asynchronously: action={}, bookId={}", event.action(), event.bookId());
        ragReindexService.reindexWithRetry();
    }
}
