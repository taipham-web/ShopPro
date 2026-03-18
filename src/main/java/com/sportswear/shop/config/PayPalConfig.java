package com.sportswear.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình PayPal - đọc từ application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "paypal")
public class PayPalConfig {

    private String clientId;
    private String clientSecret;
    private String mode = "sandbox";
    private String successUrl;
    private String cancelUrl;

    // Getters and Setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }

    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
}
