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
import com.sportswear.shop.service.MomoService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý thanh toán MoMo
 * Flow: POST /momo/create → redirect MoMo payUrl → GET /momo/return
 *       MoMo cũng gọi POST /momo/ipn (server-to-server) để xác nhận
 */
@Controller
@RequestMapping("/momo")
public class MomoController {

    private static final Logger log = LoggerFactory.getLogger(MomoController.class);

    @Autowired private MomoService momoService;
    @Autowired private CartService cartService;
    @Autowired private IOrderService orderService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;

    /**
     * Bước 1: Nhận thông tin checkout → lưu session → gọi MoMo API → redirect payUrl
     */
    @PostMapping("/create")
    public String createPayment(
            @RequestParam String shippingName,
            @RequestParam String shippingPhone,
            @RequestParam String shippingAddress,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetails userDetails,
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

            // ===== Tạo Order PENDING =====
            Order pendingOrder = new Order();
            pendingOrder.setUser(user);
            pendingOrder.setShippingName(shippingName);
            pendingOrder.setShippingPhone(shippingPhone);
            pendingOrder.setShippingAddress(shippingAddress);
            pendingOrder.setNote(note);
            pendingOrder.setShippingFee(shippingFee);
            pendingOrder.setTotalAmount(totalAmount);
            pendingOrder.setPaymentMethod(Order.PaymentMethod.MOMO);
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

            // ===== Lưu vào session =====
            String orderCode = "MOMO" + System.currentTimeMillis();
            session.setAttribute("pendingMomoOrder", pendingOrder);
            session.setAttribute("momoCartEmail", userEmail);
            session.setAttribute("momoOrderCode", orderCode);

            // ===== Gọi MoMo API tạo payment =====
            String orderInfo = "Thanh toan don hang " + orderCode;
            String payUrl = momoService.createPayment(orderCode, totalAmount, orderInfo);

            if (payUrl == null) {
                log.error("❌ Không tạo được MoMo payUrl");
                return "redirect:/checkout?error=momo_create_failed";
            }

            log.info("🔗 Redirect to MoMo: {} — amount: {}đ", orderCode, totalAmount);
            return "redirect:" + payUrl;

        } catch (Exception e) {
            log.error("❌ Lỗi tạo MoMo payment", e);
            return "redirect:/checkout?error=momo_create_failed";
        }
    }

    /**
     * Bước 2: MoMo redirect về sau khi user thanh toán
     * Verify signature → Lưu order → Xóa cart → Trang thành công
     */
    @GetMapping("/return")
    public String handleReturn(
            @RequestParam Map<String, String> allParams,
            HttpSession session,
            Model model) {

        String resultCode = allParams.getOrDefault("resultCode", "-1");
        String orderId    = allParams.getOrDefault("orderId", "");
        String transId    = allParams.getOrDefault("transId", "");
        log.info("📩 MoMo return: resultCode={}, orderId={}", resultCode, orderId);

        try {
            // ===== 1. Verify chữ ký =====
            if (!momoService.verifySignature(allParams)) {
                log.warn("⚠️ MoMo signature invalid!");
                return "redirect:/checkout?error=momo_invalid_signature";
            }

            // ===== 2. Thanh toán thành công (resultCode = 0) =====
            if (!"0".equals(resultCode)) {
                log.warn("❌ MoMo payment failed: code={}", resultCode);
                clearMomoSession(session);
                return "redirect:/checkout?error=momo_payment_failed";
            }

            // ===== 3. Lấy pending order =====
            Order pendingOrder = (Order) session.getAttribute("pendingMomoOrder");
            String userEmail   = (String) session.getAttribute("momoCartEmail");

            if (pendingOrder == null) {
                log.warn("⚠️ Pending MoMo order not found in session: {}", orderId);
                return "redirect:/checkout?error=momo_session_expired";
            }

            // ===== 4. Lưu vào DB =====
            pendingOrder.setPaid(true);
            pendingOrder.setStatus(Order.OrderStatus.CONFIRMED);
            Order savedOrder = orderService.save(pendingOrder);
            log.info("✅ MoMo order saved: {} — transId: {}", savedOrder.getOrderCode(), transId);

            // ===== 5. Xóa giỏ hàng =====
            if (userEmail != null) {
                cartService.clearCart(userEmail);
                log.info("🗑️ Cart cleared for: {}", userEmail);
            }

            clearMomoSession(session);

            // ===== 6. Trang thành công =====
            model.addAttribute("orderCode",     savedOrder.getOrderCode());
            model.addAttribute("totalAmount",   savedOrder.getTotalAmount());
            model.addAttribute("transactionNo", transId);
            model.addAttribute("paymentMethod", "MOMO");
            model.addAttribute("message", "Thanh toán MoMo thành công! Đơn hàng của bạn đang được xử lý.");

            return "user/checkout-success";

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý MoMo return", e);
            return "redirect:/checkout?error=momo_return_error";
        }
    }

    /**
     * IPN (Instant Payment Notification) — MoMo gọi server-to-server
     * Phản hồi 200 OK để MoMo biết đã nhận được
     */
    @PostMapping("/ipn")
    @ResponseBody
    public ResponseEntity<String> handleIpn(@RequestBody Map<String, String> ipnData) {
        log.info("📨 MoMo IPN received: resultCode={}, orderId={}",
                ipnData.get("resultCode"), ipnData.get("orderId"));

        if (!momoService.verifySignature(ipnData)) {
            log.warn("⚠️ MoMo IPN signature invalid");
            return ResponseEntity.ok("{\"resultCode\":\"97\",\"message\":\"Invalid signature\"}");
        }

        // IPN nhận được sau /return, chỉ cần trả 200 OK cho MoMo
        return ResponseEntity.ok("{\"resultCode\":\"0\",\"message\":\"Success\"}");
    }

    private void clearMomoSession(HttpSession session) {
        session.removeAttribute("pendingMomoOrder");
        session.removeAttribute("momoCartEmail");
        session.removeAttribute("momoOrderCode");
    }
}
