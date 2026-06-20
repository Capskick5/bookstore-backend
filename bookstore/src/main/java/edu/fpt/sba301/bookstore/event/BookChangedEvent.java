package edu.fpt.sba301.bookstore.event;

public record BookChangedEvent(
        Long bookId,
        String action
) {}
