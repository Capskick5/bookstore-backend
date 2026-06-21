package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.ai.RagClient;
import edu.fpt.sba301.bookstore.service.RagReindexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagReindexServiceImpl implements RagReindexService {

    private static final int MAX_ATTEMPTS = 3;

    private final RagClient ragClient;

    @Override
    public void reindexWithRetry() {
        long delayMs = 500L;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (ragClient.triggerIngest()) {
                log.info("RAG reindex completed on attempt {}", attempt);
                return;
            }
            if (attempt < MAX_ATTEMPTS) {
                sleep(delayMs * attempt);
            }
        }
        log.error("RAG reindex failed after {} attempts", MAX_ATTEMPTS);
    }

    @Override
    @Async
    public void reindexAsync() {
        reindexWithRetry();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
