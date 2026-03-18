package com.sportswear.shop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình MoMo từ application.properties
 * Sandbox docs: https://developers.momo.vn/v3/docs/payment/api/payment-api/
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "momo")
public class MomoConfig {

    /** Mã đối tác (Partner Code) */
    private String partnerCode;

    /** Access Key */
    private String accessKey;

    /** Secret Key (dùng để tạo HMAC-SHA256) */
    private String secretKey;

    /** URL API tạo giao dịch MoMo */
    private String apiUrl;

    /** URL redirect sau khi user thanh toán xong */
    private String redirectUrl;

    /** URL MoMo gọi để thông báo kết quả (IPN) */
    private String ipnUrl;

    /** Loại request: payWithMethod / captureWallet */
    private String requestType;
}
