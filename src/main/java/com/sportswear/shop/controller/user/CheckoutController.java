package com.sportswear.shop.controller.user;

import com.sportswear.shop.dto.CartItemDTO;
import com.sportswear.shop.entity.Order;
import com.sportswear.shop.entity.OrderDetail;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.IOrderService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller thanh toán - xử lý đặt hàng COD/VNPAY/MOMO
 * (PayPal được xử lý riêng trong PayPalController)
 */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Hiển thị trang checkout
     */
    @GetMapping
    public String checkoutPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email).orElse(null);
        model.addAttribute("user", user);

        List<CartItemDTO> cartItems = cartService.getCart(email);
        BigDecimal cartTotal = cartService.getCartTotal(email);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);

        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        return "user/checkout";
    }

    /**
     * Xử lý đặt hàng COD / VNPAY / MOMO
     * (PayPal được redirect sang /paypal/create trước đó bởi JS trong checkout.html)
     */
    @PostMapping("/process")
    public String processCheckout(
            @RequestParam String shippingName,
            @RequestParam String shippingPhone,
            @RequestParam(required = false) String email,
            @RequestParam String shippingAddress,
            @RequestParam(required = false) String note,
            @RequestParam(defaultValue = "COD") String paymentMethod,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpSession session,
            Model model) {

        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        try {
            String userEmail = userDetails.getUsername();
            User user = userRepository.findByEmail(userEmail).orElseThrow();

            // Lấy giỏ hàng
            List<CartItemDTO> cartItems = cartService.getCart(userEmail);
            if (cartItems.isEmpty()) {
                return "redirect:/cart";
            }

            BigDecimal cartTotal = cartService.getCartTotal(userEmail);

            // Tính phí ship: miễn phí nếu đơn >= 500,000đ
            BigDecimal shippingFee = cartTotal.compareTo(BigDecimal.valueOf(500_000)) >= 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(30_000);
            BigDecimal totalAmount = cartTotal.add(shippingFee);

            // ===== Tạo Order =====
            Order order = new Order();
            order.setUser(user);
            order.setShippingName(shippingName);
            order.setShippingPhone(shippingPhone);
            order.setShippingAddress(shippingAddress);
            order.setNote(note);
            order.setShippingFee(shippingFee);
            order.setTotalAmount(totalAmount);

            // Parse payment method
            try {
                order.setPaymentMethod(Order.PaymentMethod.valueOf(paymentMethod.toUpperCase()));
            } catch (IllegalArgumentException e) {
                order.setPaymentMethod(Order.PaymentMethod.COD);
            }

            // COD → chưa thanh toán, các phương thức khác → có thể đánh dấu pending
            order.setPaid(false);
            order.setStatus(Order.OrderStatus.PENDING);

            // ===== Thêm Order Details =====
            for (CartItemDTO item : cartItems) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product == null) continue;

                OrderDetail detail = new OrderDetail();
                detail.setOrder(order);
                detail.setProduct(product);
                detail.setPrice(item.getPrice());
                detail.setQuantity(item.getQuantity());
                detail.setSubtotal(item.getSubtotal());
                detail.setSize(item.getSize());
                order.getOrderDetails().add(detail);
            }

            // ===== Lưu Order vào DB =====
            Order savedOrder = orderService.save(order);
            log.info("✅ Đặt hàng thành công: {} - {} - {}đ",
                    savedOrder.getOrderCode(), paymentMethod, totalAmount);

            // ===== Xóa giỏ hàng =====
            cartService.clearCart(userEmail);
            log.info("🗑️ Đã xóa giỏ hàng của user: {}", userEmail);

            // ===== Truyền thông tin cho trang thành công =====
            model.addAttribute("orderCode", savedOrder.getOrderCode());
            model.addAttribute("totalAmount", savedOrder.getTotalAmount());
            model.addAttribute("message", "Đặt hàng thành công! Chúng tôi sẽ liên hệ xác nhận sớm.");

            return "user/checkout-success";

        } catch (Exception e) {
            log.error("❌ Lỗi khi đặt hàng", e);
            return "redirect:/checkout?error=order_failed";
        }
    }

    @GetMapping("/success")
    public String checkoutSuccess(Model model) {
        return "user/checkout-success";
    }

    @GetMapping("/cancel")
    public String checkoutCancel() {
        return "user/checkout-cancel";
    }
}
