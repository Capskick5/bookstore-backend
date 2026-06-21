package edu.fpt.sba301.bookstore.constant;

public final class PaymentMethods {
    public static final String MOCK = "mock";
    public static final String PAYOS = "payos";
    public static final String COD = "cod";

    private PaymentMethods() {
    }

    public static boolean isSupported(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return false;
        }
        String normalized = paymentMethod.toLowerCase();
        return MOCK.equals(normalized) || PAYOS.equals(normalized) || COD.equals(normalized);
    }
}
