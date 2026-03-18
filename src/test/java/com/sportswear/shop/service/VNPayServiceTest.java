package com.sportswear.shop.service;

import com.sportswear.shop.config.VNPayConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VNPayServiceTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @InjectMocks
    private VNPayService vnPayService;

    @Test
    void createPaymentUrlAndVerifySignature_shouldBeValidAndDetectTampering() {
        when(vnPayConfig.getTmnCode()).thenReturn("TMN123");
        when(vnPayConfig.getHashSecret()).thenReturn("secret-key");
        when(vnPayConfig.getPaymentUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:8080/vnpay/return");

        String paymentUrl = vnPayService.createPaymentUrl(
                "ORD-001",
                new BigDecimal("430000"),
                "Thanh toan don hang",
                "127.0.0.1");

        assertThat(paymentUrl).contains("vnp_SecureHash=");
        assertThat(paymentUrl).contains("vnp_TxnRef=ORD-001");

        String query = paymentUrl.substring(paymentUrl.indexOf('?') + 1);
        Map<String, String> params = Arrays.stream(query.split("&"))
                .map(p -> p.split("=", 2))
                .collect(Collectors.toMap(
                        kv -> URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        kv -> kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "",
                        (a, b) -> b));

        assertThat(vnPayService.verifySignature(params)).isTrue();

        params.put("vnp_Amount", "1");
        assertThat(vnPayService.verifySignature(params)).isFalse();
    }

    @Test
    void getIpAddress_shouldPrioritizeProxyHeadersAndNormalizeIpv6Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "");
        request.addHeader("Proxy-Client-IP", "unknown");
        request.addHeader("WL-Proxy-Client-IP", "0:0:0:0:0:0:0:1");
        request.setRemoteAddr("10.0.0.1");

        String ip = vnPayService.getIpAddress(request);

        assertThat(ip).isEqualTo("127.0.0.1");
    }
}
