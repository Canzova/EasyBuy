package com.easybuy.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.key.id:rzp_test_mockKeyId123456}")
    private String keyId;

    @Value("${razorpay.key.secret:mockSecretKeyRazorpay987654}")
    private String keySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${razorpay.webhook.secret:mockWebhookSecret123456}")
    private String webhookSecret;

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            log.info("Initializing RazorpayClient with Key ID: {}", keyId);
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            log.error("Failed to initialize RazorpayClient: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize Razorpay Client", e);
        }
    }

    /**
     * Checks whether placeholder / mock credentials are being used.
     * When mock credentials are detected, the service gracefully simulates
     * gateway calls so development and local testing can proceed without 401 errors.
     * Once the user adds real credentials from the Razorpay dashboard,
     * it automatically switches to live Razorpay network calls.
     */
    public boolean isMockMode() {
        return keyId == null || keyId.isBlank() || keyId.contains("mock")
                || keySecret == null || keySecret.contains("mock");
    }
}
