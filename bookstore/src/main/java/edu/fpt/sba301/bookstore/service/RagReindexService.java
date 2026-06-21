package edu.fpt.sba301.bookstore.service;

public interface RagReindexService {
    void reindexWithRetry();

    void reindexAsync();
}
