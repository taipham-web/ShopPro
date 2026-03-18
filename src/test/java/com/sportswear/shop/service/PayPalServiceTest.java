package com.sportswear.shop.service;

import com.sportswear.shop.config.PayPalConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayPalServiceTest {

    @Mock
    private PayPalConfig payPalConfig;

    @InjectMocks
    private PayPalService payPalService;

    @SuppressWarnings("unchecked")
    @Test
    void buildOrderRequest_shouldContainExpectedPaypalStructure() {
        when(payPalConfig.getSuccessUrl()).thenReturn("http://localhost:8080/paypal/success");
        when(payPalConfig.getCancelUrl()).thenReturn("http://localhost:8080/paypal/cancel");

        Map<String, Object> request = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                payPalService, "buildOrderRequest", "17.20", "ORD-001");

        assertThat(request.get("intent")).isEqualTo("CAPTURE");

        List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) request.get("purchase_units");
        assertThat(purchaseUnits).hasSize(1);
        assertThat(purchaseUnits.get(0).get("reference_id")).isEqualTo("ORD-001");

        Map<String, Object> amount = (Map<String, Object>) purchaseUnits.get(0).get("amount");
        assertThat(amount.get("currency_code")).isEqualTo("USD");
        assertThat(amount.get("value")).isEqualTo("17.20");

        Map<String, Object> appContext = (Map<String, Object>) request.get("application_context");
        assertThat(appContext.get("return_url")).isEqualTo("http://localhost:8080/paypal/success");
        assertThat(appContext.get("cancel_url")).isEqualTo("http://localhost:8080/paypal/cancel");
        assertThat(appContext.get("user_action")).isEqualTo("PAY_NOW");
    }

    @Test
    void init_shouldCreateWebClientForConfiguredMode() {
        when(payPalConfig.getMode()).thenReturn("live");

        payPalService.init();

        Object webClient = ReflectionTestUtils.getField(payPalService, "webClient");
        assertThat(webClient).isNotNull();
    }
}
