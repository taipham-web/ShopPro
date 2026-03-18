package com.sportswear.shop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình VNPay từ application.properties
 * Lấy credentials tại: https://sandbox.vnpayment.vn/devreg/
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {

    /** Mã merchant (Terminal ID) — lấy từ VNPay Merchant Portal */
    private String tmnCode;

    /** Khóa bí mật để tạo chữ ký HMAC-SHA512 */
    private String hashSecret;

    /** URL trang thanh toán VNPay (sandbox/production) */
    private String paymentUrl;

    /** URL trả về sau khi user hoàn tất/hủy thanh toán */
    private String returnUrl;
}
