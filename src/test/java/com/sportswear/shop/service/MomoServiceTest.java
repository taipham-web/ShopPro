package com.sportswear.shop.service;

import com.sportswear.shop.config.MomoConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomoServiceTest {

    @Mock
    private MomoConfig momoConfig;

    @InjectMocks
    private MomoService momoService;

    @Test
    void verifySignature_shouldReturnTrueWhenSignatureMatches() throws Exception {
        when(momoConfig.getAccessKey()).thenReturn("access-key");
        when(momoConfig.getSecretKey()).thenReturn("secret-key");

        Map<String, String> params = new HashMap<>();
        params.put("amount", "430000");
        params.put("extraData", "");
        params.put("message", "Successful.");
        params.put("orderId", "ORD-001");
        params.put("orderInfo", "Thanh toan");
        params.put("orderType", "momo_wallet");
        params.put("partnerCode", "MOMO");
        params.put("payType", "qr");
        params.put("requestId", "REQ-1");
        params.put("responseTime", "1710000000");
        params.put("resultCode", "0");
        params.put("transId", "999");

        String rawSignature = "accessKey=access-key"
                + "&amount=430000"
                + "&extraData="
                + "&message=Successful."
                + "&orderId=ORD-001"
                + "&orderInfo=Thanh toan"
                + "&orderType=momo_wallet"
                + "&partnerCode=MOMO"
                + "&payType=qr"
                + "&requestId=REQ-1"
                + "&responseTime=1710000000"
                + "&resultCode=0"
                + "&transId=999";

        params.put("signature", hmacSha256("secret-key", rawSignature));

        assertThat(momoService.verifySignature(params)).isTrue();
    }

    @Test
    void verifySignature_shouldReturnFalseWhenSignatureInvalid() {
        when(momoConfig.getAccessKey()).thenReturn("access-key");
        when(momoConfig.getSecretKey()).thenReturn("secret-key");

        Map<String, String> params = new HashMap<>();
        params.put("amount", "430000");
        params.put("orderId", "ORD-001");
        params.put("partnerCode", "MOMO");
        params.put("signature", "wrong-signature");

        assertThat(momoService.verifySignature(params)).isFalse();
    }

    private String hmacSha256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) {
                hex.append('0');
            }
            hex.append(h);
        }
        return hex.toString();
    }
}
