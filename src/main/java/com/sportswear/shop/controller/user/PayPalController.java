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
import com.sportswear.shop.service.PayPalService;
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
import java.util.Map;

/**
 * Controller xử lý PayPal payment flow
 */
@Controller
@RequestMapping("/paypal")
public class PayPalController {

    private static final Logger log = LoggerFactory.getLogger(PayPalController.class);

    @Autowired
    private PayPalService payPalService;

    @Autowired
    private CartService cartService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Bước 1: Tạo PayPal order và redirect sang trang PayPal
     */
    @PostMapping("/create")
    public String createPayment(
            @RequestParam String shippingName,
            @RequestParam String shippingPhone,
            @RequestParam(required = false) String email,
            @RequestParam String shippingAddress,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpSession session) {

        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        try {
            String userEmail = userDetails.getUsername();
            User user = userRepository.findByEmail(userEmail).orElseThrow();

            // Lấy giỏ hàng và tính tổng
            List<CartItemDTO> cartItems = cartService.getCart(userEmail);
            BigDecimal cartTotal = cartService.getCartTotal(userEmail);

            if (cartItems.isEmpty()) {
                return "redirect:/cart";
            }

            // Tạo order tạm thời lưu vào session
            Order pendingOrder = new Order();
            pendingOrder.setUser(user);
            pendingOrder.setShippingName(shippingName);
            pendingOrder.setShippingPhone(shippingPhone);
            pendingOrder.setShippingAddress(shippingAddress);
            pendingOrder.setNote(note);
            pendingOrder.setPaymentMethod(Order.PaymentMethod.PAYPAL);

            // Tính phí ship
            BigDecimal shippingFee = cartTotal.compareTo(BigDecimal.valueOf(500000)) >= 0
                    ? BigDecimal.ZERO : BigDecimal.valueOf(30000);
            BigDecimal totalAmount = cartTotal.add(shippingFee);
            pendingOrder.setTotalAmount(totalAmount);
            pendingOrder.setShippingFee(shippingFee);

            // Thêm order details
            for (CartItemDTO item : cartItems) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product == null) continue;
                OrderDetail detail = new OrderDetail();
                detail.setOrder(pendingOrder);
                detail.setProduct(product);
                detail.setPrice(item.getPrice());
                detail.setQuantity(item.getQuantity());
                detail.setSubtotal(item.getSubtotal());
                pendingOrder.getOrderDetails().add(detail);
            }

            // Lưu order tạm vào session để dùng sau khi PayPal callback về
            session.setAttribute("pendingPaypalOrder", pendingOrder);
            session.setAttribute("paypalCartEmail", userEmail);

            // Tạo PayPal payment và lấy approval URL
            String tempOrderId = "TEMP-" + System.currentTimeMillis();
            String approvalUrl = payPalService.createOrder(totalAmount, "VND", tempOrderId);

            return "redirect:" + approvalUrl;

        } catch (Exception e) {
            log.error("Lỗi tạo PayPal order", e);
            return "redirect:/checkout?error=paypal_create_failed";
        }
    }

    /**
     * Bước 2: PayPal callback sau khi user approve - capture payment
     */
    @GetMapping("/success")
    public String paymentSuccess(
            @RequestParam("token") String paypalOrderId,
            @RequestParam(value = "PayerID", required = false) String payerId,
            HttpSession session,
            Model model) {

        try {
            // Capture payment từ PayPal
            Map<?, ?> captureResult = payPalService.captureOrder(paypalOrderId);
            String status = (String) captureResult.get("status");

            if (!"COMPLETED".equalsIgnoreCase(status)) {
                log.warn("PayPal capture status: {}", status);
                return "redirect:/checkout?error=paypal_not_completed";
            }

            // Lấy pending order từ session và lưu vào DB
            Order pendingOrder = (Order) session.getAttribute("pendingPaypalOrder");
            String userEmail = (String) session.getAttribute("paypalCartEmail");

            if (pendingOrder != null) {
                pendingOrder.setPaid(true);
                pendingOrder.setStatus(Order.OrderStatus.CONFIRMED);
                Order savedOrder = orderService.save(pendingOrder);

                // Xóa giỏ hàng bằng clearCart (nhanh hơn và đảm bảo sạch)
                if (userEmail != null) {
                    cartService.clearCart(userEmail);
                }

                // Xóa session
                session.removeAttribute("pendingPaypalOrder");
                session.removeAttribute("paypalCartEmail");

                model.addAttribute("orderCode", savedOrder.getOrderCode());
                model.addAttribute("totalAmount", savedOrder.getTotalAmount());
                model.addAttribute("paypalOrderId", paypalOrderId);
                model.addAttribute("message", "Thanh toán PayPal thành công!");
            }

            return "user/checkout-success";

        } catch (Exception e) {
            log.error("Lỗi capture PayPal payment", e);
            return "redirect:/checkout?error=paypal_capture_failed";
        }
    }

    /**
     * Bước 3: User hủy thanh toán trên trang PayPal
     */
    @GetMapping("/cancel")
    public String paymentCancel(HttpSession session) {
        session.removeAttribute("pendingPaypalOrder");
        session.removeAttribute("paypalCartEmail");
        return "redirect:/checkout?error=paypal_cancelled";
    }
}
