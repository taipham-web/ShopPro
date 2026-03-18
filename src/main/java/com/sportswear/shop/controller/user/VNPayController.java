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
import com.sportswear.shop.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
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
 * Controller xử lý thanh toán VNPay
 * Flow: POST /vnpay/create → redirect VNPay → GET /vnpay/return
 */
@Controller
@RequestMapping("/vnpay")
public class VNPayController {

    private static final Logger log = LoggerFactory.getLogger(VNPayController.class);

    @Autowired private VNPayService vnPayService;
    @Autowired private CartService cartService;
    @Autowired private IOrderService orderService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;

    /**
     * Bước 1: Nhận thông tin checkout → lưu vào session → tạo URL VNPay → redirect
     */
    @PostMapping("/create")
    public String createPayment(
            @RequestParam String shippingName,
            @RequestParam String shippingPhone,
            @RequestParam String shippingAddress,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpSession session) {

        if (userDetails == null) return "redirect:/auth/login";

        try {
            String userEmail = userDetails.getUsername();
            User user = userRepository.findByEmail(userEmail).orElseThrow();

            // Lấy giỏ hàng
            List<CartItemDTO> cartItems = cartService.getCart(userEmail);
            if (cartItems.isEmpty()) return "redirect:/cart";

            BigDecimal cartTotal = cartService.getCartTotal(userEmail);
            BigDecimal shippingFee = cartTotal.compareTo(BigDecimal.valueOf(500_000)) >= 0
                    ? BigDecimal.ZERO : BigDecimal.valueOf(30_000);
            BigDecimal totalAmount = cartTotal.add(shippingFee);

            // ===== Tạo Order PENDING (chưa lưu DB) =====
            Order pendingOrder = new Order();
            pendingOrder.setUser(user);
            pendingOrder.setShippingName(shippingName);
            pendingOrder.setShippingPhone(shippingPhone);
            pendingOrder.setShippingAddress(shippingAddress);
            pendingOrder.setNote(note);
            pendingOrder.setShippingFee(shippingFee);
            pendingOrder.setTotalAmount(totalAmount);
            pendingOrder.setPaymentMethod(Order.PaymentMethod.VNPAY);
            pendingOrder.setPaid(false);
            pendingOrder.setStatus(Order.OrderStatus.PENDING);

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

            // Lưu vào session để dùng lại sau callback
            session.setAttribute("pendingVnpayOrder", pendingOrder);
            session.setAttribute("vnpayCartEmail", userEmail);

            // Tạo URL thanh toán VNPay
            String orderCode = "VNP" + System.currentTimeMillis();
            session.setAttribute("vnpayOrderRef", orderCode);

            String ipAddress = vnPayService.getIpAddress(request);
            String orderInfo = "Thanh toan don hang " + orderCode;
            String paymentUrl = vnPayService.createPaymentUrl(orderCode, totalAmount, orderInfo, ipAddress);

            log.info("🔗 Redirect to VNPay: {} — amount: {}đ", orderCode, totalAmount);
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            log.error("❌ Lỗi tạo VNPay URL", e);
            return "redirect:/checkout?error=vnpay_create_failed";
        }
    }

    /**
     * Bước 2: VNPay redirect về sau khi thanh toán
     * Verify signature → Lưu order → Xóa giỏ hàng → Trang thành công
     */
    @GetMapping("/return")
    public String handleReturn(
            @RequestParam Map<String, String> allParams,
            HttpSession session,
            Model model) {

        try {
            log.info("📩 VNPay return: responseCode={}", allParams.get("vnp_ResponseCode"));

            // ===== 1. Verify chữ ký =====
            if (!vnPayService.verifySignature(allParams)) {
                log.warn("⚠️ VNPay signature invalid!");
                return "redirect:/checkout?error=vnpay_invalid_signature";
            }

            String responseCode = allParams.get("vnp_ResponseCode");
            String txnRef      = allParams.get("vnp_TxnRef");
            String transactionNo = allParams.getOrDefault("vnp_TransactionNo", "");

            // ===== 2. Kiểm tra thanh toán thành công (responseCode = "00") =====
            if (!"00".equals(responseCode)) {
                log.warn("❌ VNPay payment failed: code={} ref={}", responseCode, txnRef);
                session.removeAttribute("pendingVnpayOrder");
                session.removeAttribute("vnpayCartEmail");
                session.removeAttribute("vnpayOrderRef");
                return "redirect:/checkout?error=vnpay_payment_failed";
            }

            // ===== 3. Lưu Order vào DB =====
            Order pendingOrder = (Order) session.getAttribute("pendingVnpayOrder");
            String userEmail   = (String) session.getAttribute("vnpayCartEmail");

            if (pendingOrder == null) {
                log.warn("⚠️ Pending order not found in session for txnRef: {}", txnRef);
                return "redirect:/checkout?error=vnpay_session_expired";
            }

            pendingOrder.setPaid(true);
            pendingOrder.setStatus(Order.OrderStatus.CONFIRMED);
            Order savedOrder = orderService.save(pendingOrder);
            log.info("✅ VNPay order saved: {} — transaction: {}", savedOrder.getOrderCode(), transactionNo);

            // ===== 4. Xóa giỏ hàng =====
            if (userEmail != null) {
                cartService.clearCart(userEmail);
                log.info("🗑️ Cart cleared for: {}", userEmail);
            }

            // ===== 5. Xóa session =====
            session.removeAttribute("pendingVnpayOrder");
            session.removeAttribute("vnpayCartEmail");
            session.removeAttribute("vnpayOrderRef");

            // ===== 6. Redirect trang thành công =====
            model.addAttribute("orderCode",    savedOrder.getOrderCode());
            model.addAttribute("totalAmount",  savedOrder.getTotalAmount());
            model.addAttribute("transactionNo", transactionNo);
            model.addAttribute("paymentMethod", "VNPAY");
            model.addAttribute("message", "Thanh toán VNPay thành công! Đơn hàng của bạn đang được xử lý.");

            return "user/checkout-success";

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý VNPay return", e);
            return "redirect:/checkout?error=vnpay_return_error";
        }
    }
}
