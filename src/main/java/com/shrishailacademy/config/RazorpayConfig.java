package com.shrishailacademy.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RazorpayConfig {

    @Bean
    @ConditionalOnProperty(name = "razorpay.enabled", havingValue = "true")
    public RazorpayClient razorpayClient(
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret) {
        if (!StringUtils.hasText(keyId) || !StringUtils.hasText(keySecret)) {
            throw new IllegalStateException("Razorpay is enabled but key ID/secret are not configured");
        }

        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            throw new IllegalStateException("Failed to initialize Razorpay client", e);
        }
    }
}