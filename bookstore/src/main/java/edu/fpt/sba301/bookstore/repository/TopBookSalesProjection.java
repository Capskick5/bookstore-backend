package edu.fpt.sba301.bookstore.repository;

public interface TopBookSalesProjection {
    Long getBookId();

    String getTitle();

    Long getSoldCount();
}
