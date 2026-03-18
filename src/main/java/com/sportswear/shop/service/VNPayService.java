package com.sportswear.shop.service;

import com.sportswear.shop.config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service tích hợp VNPay Payment Gateway
 * Tài liệu API: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
 */
@Service
public class VNPayService {

    private static final Logger log = LoggerFactory.getLogger(VNPayService.class);

    @Autowired
    private VNPayConfig vnPayConfig;

    /**
     * Tạo URL thanh toán VNPay
     *
     * @param orderCode  Mã đơn hàng (dùng làm vnp_TxnRef)
     * @param amount     Số tiền VNĐ (VNPay nhận x100)
     * @param orderInfo  Thông tin đơn hàng
     * @param ipAddress  IP của client
     * @return URL redirect đến trang thanh toán VNPay
     */
    public String createPaymentUrl(String orderCode, BigDecimal amount,
                                   String orderInfo, String ipAddress) {
        try {
            String vnpVersion = "2.1.0";
            String vnpCommand = "pay";
            String vnpLocale = "vn";
            String vnpCurrCode = "VND";
            String vnpOrderType = "other";

            // VNPay yêu cầu số tiền * 100 (không có dấu thập phân)
            long vnpAmount = amount.longValue() * 100L;

            // Thời điểm tạo giao dịch & hết hạn (15 phút)
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = fmt.format(cal.getTime());
            cal.add(Calendar.MINUTE, 15);
            String vnpExpireDate = fmt.format(cal.getTime());

            // Truncate orderInfo nếu quá dài
            String safeOrderInfo = orderInfo.length() > 255
                    ? orderInfo.substring(0, 255) : orderInfo;

            // ===== Build params (PHẢI sort theo alphabet) =====
            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version",    vnpVersion);
            vnpParams.put("vnp_Command",    vnpCommand);
            vnpParams.put("vnp_TmnCode",    vnPayConfig.getTmnCode());
            vnpParams.put("vnp_Amount",     String.valueOf(vnpAmount));
            vnpParams.put("vnp_CurrCode",   vnpCurrCode);
            vnpParams.put("vnp_TxnRef",     orderCode);
            vnpParams.put("vnp_OrderInfo",  safeOrderInfo);
            vnpParams.put("vnp_OrderType",  vnpOrderType);
            vnpParams.put("vnp_Locale",     vnpLocale);
            vnpParams.put("vnp_ReturnUrl",  vnPayConfig.getReturnUrl());
            vnpParams.put("vnp_IpAddr",     ipAddress);
            vnpParams.put("vnp_CreateDate", vnpCreateDate);
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // ===== Build query string & tạo signature =====
            StringBuilder queryBuilder = new StringBuilder();
            StringBuilder hashDataBuilder = new StringBuilder();

            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value != null && !value.isEmpty()) {
                    hashDataBuilder.append(key).append('=')
                                   .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                    queryBuilder.append(URLEncoder.encode(key, StandardCharsets.US_ASCII))
                                .append('=')
                                .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                    hashDataBuilder.append('&');
                    queryBuilder.append('&');
                }
            }

            // Bỏ '&' cuối
            String hashData = hashDataBuilder.substring(0, hashDataBuilder.length() - 1);
            String queryString = queryBuilder.substring(0, queryBuilder.length() - 1);

            // Tạo chữ ký HMAC-SHA512
            String secureHash = hmacSha512(vnPayConfig.getHashSecret(), hashData);

            String paymentUrl = vnPayConfig.getPaymentUrl()
                    + "?" + queryString
                    + "&vnp_SecureHash=" + secureHash;

            log.info("✅ VNPay URL created for order: {}", orderCode);
            return paymentUrl;

        } catch (Exception e) {
            log.error("❌ Lỗi tạo VNPay URL", e);
            throw new RuntimeException("Không thể tạo URL thanh toán VNPay", e);
        }
    }

    /**
     * Xác thực chữ ký VNPay trả về (return URL)
     *
     * @param params Tất cả query params từ VNPay return URL
     * @return true nếu signature hợp lệ
     */
    public boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        // Loại bỏ các field không dùng để hash
        Map<String, String> hashParams = new TreeMap<>(params);
        hashParams.remove("vnp_SecureHash");
        hashParams.remove("vnp_SecureHashType");

        // Build hash data
        StringBuilder hashDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : hashParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                hashDataBuilder.append(key).append('=')
                               .append(URLEncoder.encode(value, StandardCharsets.US_ASCII))
                               .append('&');
            }
        }

        String hashData = hashDataBuilder.substring(0, hashDataBuilder.length() - 1);
        String computedHash = hmacSha512(vnPayConfig.getHashSecret(), hashData);

        boolean valid = computedHash.equalsIgnoreCase(receivedHash);
        if (!valid) {
            log.warn("⚠️ VNPay signature mismatch! Computed: {} | Received: {}", computedHash, receivedHash);
        }
        return valid;
    }

    /**
     * Lấy IP address của client từ request
     */
    public String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Xử lý IPv6 loopback
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * Tạo chữ ký HMAC-SHA512
     */
    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo HMAC-SHA512", e);
        }
    }
}
