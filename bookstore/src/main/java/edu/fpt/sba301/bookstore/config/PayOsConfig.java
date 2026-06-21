package edu.fpt.sba301.bookstore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOsConfig {

    @Bean
    @ConditionalOnProperty(name = "app.payment.payos.client-id")
    public PayOS payOS(
            @Value("${app.payment.payos.client-id}") String clientId,
            @Value("${app.payment.payos.api-key}") String apiKey,
            @Value("${app.payment.payos.checksum-key}") String checksumKey) {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
