package edu.fpt.sba301.bookstore.scheduler;

import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.service.RagReindexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagReconciliationScheduler {

    private final BookRepository bookRepository;
    private final RagReindexService ragReindexService;

    @Scheduled(cron = "${app.rag.reconciliation-cron:0 0 2 * * ?}")
    public void reconcileCatalogWithRag() {
        long activeBooks = bookRepository.countByActiveTrue();
        log.info("Starting nightly RAG reconciliation for {} active catalog books", activeBooks);
        ragReindexService.reindexWithRetry("nightly-reconciliation");
    }
}
