package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.ai.RagClient;
import edu.fpt.sba301.bookstore.service.RagReindexService;
import edu.fpt.sba301.bookstore.service.ReindexStatusService;
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
    private final ReindexStatusService reindexStatusService;

    @Override
    public void reindexWithRetry() {
        reindexWithRetry("reindex");
    }

    @Override
    public void reindexWithRetry(String source) {
        String taskId = reindexStatusService.startTask(source);
        reindexStatusService.markRunning(taskId);

        long delayMs = 500L;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (ragClient.triggerIngest()) {
                log.info("RAG reindex completed on attempt {} for source {}", attempt, source);
                reindexStatusService.markCompleted(taskId);
                return;
            }
            if (attempt < MAX_ATTEMPTS) {
                sleep(delayMs * attempt);
            }
        }

        log.error("RAG reindex failed after {} attempts for source {}", MAX_ATTEMPTS, source);
        reindexStatusService.markFailed(taskId, "RAG reindex failed after " + MAX_ATTEMPTS + " attempts");
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
