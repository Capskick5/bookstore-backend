package edu.fpt.sba301.bookstore.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class PaymentServiceFactory {

    private final List<PaymentService> paymentServices;

    @Value("${app.payment.provider:mock}")
    private String provider;

    public PaymentServiceFactory(List<PaymentService> paymentServices) {
        this.paymentServices = paymentServices;
    }

    public PaymentService getActiveService() {
        return paymentServices.stream()
                .filter(s -> s.getProviderName().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Payment provider not configured: " + provider));
    }

    public PaymentService getService(String providerName) {
        return paymentServices.stream()
                .filter(s -> s.getProviderName().equalsIgnoreCase(providerName.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown payment provider: " + providerName));
    }
}
