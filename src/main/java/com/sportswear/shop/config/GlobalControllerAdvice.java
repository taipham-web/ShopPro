package com.sportswear.shop.config;

import com.sportswear.shop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Tự động inject cartCount vào model của mọi trang (dùng cho header cart badge)
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CartService cartService;

    @ModelAttribute("cartCount")
    public int cartCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return 0;
        }
        try {
            return cartService.getCartCount(userDetails.getUsername());
        } catch (Exception e) {
            return 0;
        }
    }
}
