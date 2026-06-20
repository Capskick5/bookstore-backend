package edu.fpt.sba301.bookstore.enums;

public final class OrderStatus {
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String SHIPPED = "SHIPPED";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";

    private OrderStatus() {
    }
}
