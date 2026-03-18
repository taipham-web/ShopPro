package com.sportswear.shop;

import com.sportswear.shop.config.MomoConfig;
import com.sportswear.shop.config.PayPalConfig;
import com.sportswear.shop.config.VNPayConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({PayPalConfig.class, VNPayConfig.class, MomoConfig.class})
public class SportswearApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportswearApplication.class, args);
    }
}
