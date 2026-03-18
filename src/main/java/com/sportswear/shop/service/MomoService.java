package com.sportswear.shop.service;

import com.sportswear.shop.config.MomoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Service tích hợp MoMo Payment Gateway (API v2)
 * Tài liệu: https://developers.momo.vn/v3/docs/payment/api/payment-api/
 */
@Service
public class MomoService {

    private static final Logger log = LoggerFactory.getLogger(MomoService.class);

    @Autowired
    private MomoConfig momoConfig;

    /**
     * Tạo giao dịch MoMo và trả về payUrl để redirect
     *
     * @param orderCode Mã đơn hàng
     * @param amount    Số tiền VNĐ
     * @param orderInfo Mô tả đơn hàng
     * @return payUrl MoMo hoặc null nếu thất bại
     */
    public String createPayment(String orderCode, BigDecimal amount, String orderInfo) {
        try {
            String partnerCode = momoConfig.getPartnerCode();
            String accessKey   = momoConfig.getAccessKey();
            String secretKey   = momoConfig.getSecretKey();
            String requestId   = partnerCode + "_" + System.currentTimeMillis();
            String amountStr   = String.valueOf(amount.longValue());
            String redirectUrl = momoConfig.getRedirectUrl();
            String ipnUrl      = momoConfig.getIpnUrl();
            String requestType = momoConfig.getRequestType();
            String extraData   = "";
            String lang        = "vi";

            // ===== Build raw signature (thứ tự theo docs MoMo) =====
            String rawSignature = "accessKey=" + accessKey
                    + "&amount=" + amountStr
                    + "&extraData=" + extraData
                    + "&ipnUrl=" + ipnUrl
                    + "&orderId=" + orderCode
                    + "&orderInfo=" + orderInfo
                    + "&partnerCode=" + partnerCode
                    + "&redirectUrl=" + redirectUrl
                    + "&requestId=" + requestId
                    + "&requestType=" + requestType;

            String signature = hmacSha256(secretKey, rawSignature);

            // ===== Build JSON body thủ công (tránh phụ thuộc Jackson) =====
            String body = buildJson(partnerCode, "SportWear Shop", "SportWearStore",
                    requestId, amountStr, orderCode, orderInfo, redirectUrl,
                    ipnUrl, lang, extraData, requestType, signature);

            log.info("📤 MoMo request orderId={} amount={}", orderCode, amountStr);

            // ===== Gọi MoMo API qua HttpURLConnection =====
            String responseJson = postJson(momoConfig.getApiUrl(), body);
            log.info("📥 MoMo response: {}", responseJson);

            // ===== Parse resultCode và payUrl từ JSON thủ công =====
            int resultCode = extractIntField(responseJson, "resultCode");
            if (resultCode == 0) {
                String payUrl = extractStringField(responseJson, "payUrl");
                log.info("✅ MoMo payUrl created for order: {}", orderCode);
                return payUrl;
            } else {
                String message = extractStringField(responseJson, "message");
                log.error("❌ MoMo error: code={}, message={}", resultCode, message);
                return null;
            }

        } catch (Exception e) {
            log.error("❌ Lỗi tạo MoMo payment", e);
            return null;
        }
    }

    /**
     * Xác thực chữ ký MoMo trả về
     */
    public boolean verifySignature(Map<String, String> params) {
        try {
            String receivedSignature = params.getOrDefault("signature", "");
            String accessKey    = momoConfig.getAccessKey();
            String secretKey    = momoConfig.getSecretKey();
            String amount       = params.getOrDefault("amount", "");
            String extraData    = params.getOrDefault("extraData", "");
            String message      = params.getOrDefault("message", "");
            String orderId      = params.getOrDefault("orderId", "");
            String orderInfo    = params.getOrDefault("orderInfo", "");
            String orderType    = params.getOrDefault("orderType", "");
            String partnerCode  = params.getOrDefault("partnerCode", "");
            String payType      = params.getOrDefault("payType", "");
            String requestId    = params.getOrDefault("requestId", "");
            String responseTime = params.getOrDefault("responseTime", "");
            String resultCode   = params.getOrDefault("resultCode", "");
            String transId      = params.getOrDefault("transId", "");

            String rawSignature = "accessKey=" + accessKey
                    + "&amount=" + amount
                    + "&extraData=" + extraData
                    + "&message=" + message
                    + "&orderId=" + orderId
                    + "&orderInfo=" + orderInfo
                    + "&orderType=" + orderType
                    + "&partnerCode=" + partnerCode
                    + "&payType=" + payType
                    + "&requestId=" + requestId
                    + "&responseTime=" + responseTime
                    + "&resultCode=" + resultCode
                    + "&transId=" + transId;

            String computedSignature = hmacSha256(secretKey, rawSignature);
            boolean valid = computedSignature.equals(receivedSignature);
            if (!valid) {
                log.warn("⚠️ MoMo sig mismatch! computed={} | received={}", computedSignature, receivedSignature);
            }
            return valid;
        } catch (Exception e) {
            log.error("❌ Lỗi verify MoMo signature", e);
            return false;
        }
    }

    // ====================================================================
    // Helper methods
    // ====================================================================

    private String buildJson(String partnerCode, String partnerName, String storeId,
                             String requestId, String amount, String orderId,
                             String orderInfo, String redirectUrl, String ipnUrl,
                             String lang, String extraData, String requestType,
                             String signature) {
        // Build JSON thủ công - không cần Jackson
        return "{"
                + "\"partnerCode\":\"" + partnerCode + "\","
                + "\"partnerName\":\"" + partnerName + "\","
                + "\"storeId\":\"" + storeId + "\","
                + "\"requestId\":\"" + requestId + "\","
                + "\"amount\":" + amount + ","
                + "\"orderId\":\"" + orderId + "\","
                + "\"orderInfo\":\"" + orderInfo + "\","
                + "\"redirectUrl\":\"" + redirectUrl + "\","
                + "\"ipnUrl\":\"" + ipnUrl + "\","
                + "\"lang\":\"" + lang + "\","
                + "\"extraData\":\"" + extraData + "\","
                + "\"requestType\":\"" + requestType + "\","
                + "\"signature\":\"" + signature + "\""
                + "}";
    }

    private String postJson(String urlStr, String body) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300
                        ? conn.getInputStream()
                        : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /** Trích xuất giá trị int của field trong JSON string */
    private int extractIntField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return -1;
        int start = idx + key.length();
        int end = json.indexOf(',', start);
        if (end < 0) end = json.indexOf('}', start);
        return Integer.parseInt(json.substring(start, end).trim());
    }

    /** Trích xuất giá trị String của field trong JSON string */
    private String extractStringField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = idx + key.length();
        int end = json.indexOf('"', start);
        if (end < 0) return "";
        return json.substring(start, end);
    }

    /** Tạo chữ ký HMAC-SHA256 */
    private String hmacSha256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }
}
