package com.sportswear.shop.service;

import com.sportswear.shop.config.PayPalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service tích hợp PayPal REST API v2
 */
@Service
public class PayPalService {

    private static final Logger log = LoggerFactory.getLogger(PayPalService.class);

    // PayPal API base URLs
    private static final String SANDBOX_URL = "https://api-m.sandbox.paypal.com";
    private static final String LIVE_URL    = "https://api-m.paypal.com";

    @Autowired
    private PayPalConfig payPalConfig;

    private WebClient webClient;

    @Autowired
    public void init() {
        String baseUrl = "sandbox".equalsIgnoreCase(payPalConfig.getMode()) ? SANDBOX_URL : LIVE_URL;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    // =========================================================
    //  Lấy Access Token
    // =========================================================
    private String getAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        Map<?, ?> response = webClient.post()
                .uri("/v1/oauth2/token")
                .headers(h -> h.setBasicAuth(payPalConfig.getClientId(), payPalConfig.getClientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Không thể lấy PayPal Access Token");
        }
        return (String) response.get("access_token");
    }

    // =========================================================
    //  Tạo PayPal Order - trả về approvalUrl để redirect user
    // =========================================================
    public String createOrder(BigDecimal totalAmount, String currency, String orderId) {
        String accessToken = getAccessToken();

        // Convert VND → USD (PayPal không hỗ trợ VND trực tiếp)
        // Tỷ giá tham khảo: 1 USD ≈ 25,000 VND
        BigDecimal usdAmount = totalAmount.divide(BigDecimal.valueOf(25000), 2, RoundingMode.HALF_UP);

        Map<String, Object> orderRequest = buildOrderRequest(usdAmount.toPlainString(), orderId);

        Map<?, ?> response = webClient.post()
                .uri("/v2/checkout/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Không thể tạo PayPal order");
        }

        // Tìm link approve
        @SuppressWarnings("unchecked")
        List<Map<String, String>> links = (List<Map<String, String>>) response.get("links");
        if (links != null) {
            for (Map<String, String> link : links) {
                if ("approve".equals(link.get("rel"))) {
                    return link.get("href");
                }
            }
        }

        throw new RuntimeException("Không tìm thấy PayPal approval URL");
    }

    // =========================================================
    //  Capture Order - sau khi user approve
    // =========================================================
    public Map<?, ?> captureOrder(String paypalOrderId) {
        String accessToken = getAccessToken();

        Map<?, ?> response = webClient.post()
                .uri("/v2/checkout/orders/{orderId}/capture", paypalOrderId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new HashMap<>())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Không thể capture PayPal order");
        }
        return response;
    }

    // =========================================================
    //  Build request body cho tạo order
    // =========================================================
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildOrderRequest(String usdAmount, String internalOrderId) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("intent", "CAPTURE");

        // Purchase units
        Map<String, Object> unit = new LinkedHashMap<>();
        unit.put("reference_id", internalOrderId);

        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("currency_code", "USD");
        amount.put("value", usdAmount);
        unit.put("amount", amount);

        order.put("purchase_units", List.of(unit));

        // Application context (URLs redirect)
        Map<String, Object> appContext = new LinkedHashMap<>();
        appContext.put("return_url", payPalConfig.getSuccessUrl());
        appContext.put("cancel_url", payPalConfig.getCancelUrl());
        appContext.put("user_action", "PAY_NOW");
        appContext.put("brand_name", "SportWear Shop");
        order.put("application_context", appContext);

        return order;
    }
}
