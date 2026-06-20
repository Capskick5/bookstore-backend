package edu.fpt.sba301.bookstore.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookChangedEventListener {

    @Async
    @EventListener
    public void handleBookChangedEvent(BookChangedEvent event) {
        log.info("Received BookChangedEvent asynchronously: action={}, bookId={}", event.action(), event.bookId());
    }
}
