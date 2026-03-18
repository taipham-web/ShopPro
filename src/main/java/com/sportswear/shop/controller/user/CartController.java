package com.sportswear.shop.controller.user;

import com.sportswear.shop.dto.CartItemDTO;
import com.sportswear.shop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<CartItemDTO> cartItems = cartService.getCart(userDetails.getUsername());
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartService.getCartTotal(userDetails.getUsername()));
        return "user/cart";
    }

    @PostMapping("/add")
    @ResponseBody
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(required = false) String size,
                            @AuthenticationPrincipal UserDetails userDetails) {
        cartService.addToCart(userDetails.getUsername(), productId, quantity, size);
        return "{\"success\": true, \"count\": " + cartService.getCartCount(userDetails.getUsername()) + "}";
    }

    @PostMapping("/update")
    @ResponseBody
    public String updateCart(@RequestParam Long cartItemId,
                             @RequestParam int quantity,
                             @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        cartService.updateQuantity(email, cartItemId, quantity);
        List<CartItemDTO> cart = cartService.getCart(email);
        BigDecimal subtotal = cart.stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst().map(CartItemDTO::getSubtotal).orElse(BigDecimal.ZERO);
        BigDecimal total = cartService.getCartTotal(email);
        int count = cartService.getCartCount(email);
        boolean freeShipping = total.compareTo(new BigDecimal("500000")) >= 0;
        long grandTotal = total.longValue() + (freeShipping ? 0 : 30000);
        return String.format("{\"success\":true,\"count\":%d,\"subtotal\":%d,\"total\":%d,\"grandTotal\":%d,\"freeShipping\":%s}",
                count, subtotal.longValue(), total.longValue(), grandTotal, freeShipping);
    }

    @PostMapping("/remove")
    @ResponseBody
    public String removeFromCart(@RequestParam Long cartItemId,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        cartService.removeFromCart(email, cartItemId);
        BigDecimal total = cartService.getCartTotal(email);
        int count = cartService.getCartCount(email);
        boolean freeShipping = total.compareTo(new BigDecimal("500000")) >= 0;
        long grandTotal = total.longValue() + (freeShipping ? 0 : 30000);
        return String.format("{\"success\":true,\"count\":%d,\"total\":%d,\"grandTotal\":%d,\"freeShipping\":%s,\"isEmpty\":%s}",
                count, total.longValue(), grandTotal, freeShipping, count == 0);
    }

    @GetMapping("/count")
    @ResponseBody
    public String getCartCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "{\"count\": 0}";
        return "{\"count\": " + cartService.getCartCount(userDetails.getUsername()) + "}";
    }
}
