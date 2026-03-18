package com.sportswear.shop.controller.user;

import com.sportswear.shop.entity.Order;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.OrderRepository;
import com.sportswear.shop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller trang đơn hàng của user
 */
@Controller
@RequestMapping("/user/orders")
public class UserOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Danh sách đơn hàng của user
     */
    @GetMapping
    public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElse(null);

        List<Order> orders = orderRepository.findByUserEmailOrderByCreatedAtDesc(email);

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("pendingCount", orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count());
        model.addAttribute("shippingCount", orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.SHIPPING).count());
        model.addAttribute("deliveredCount", orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED).count());

        return "user/my-orders";
    }

    /**
     * Chi tiết một đơn hàng của user
     */
    @GetMapping("/{orderCode}")
    public String orderDetail(@PathVariable String orderCode,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElse(null);

        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);

        // Bảo mật: chỉ cho xem đơn hàng của chính mình
        if (order == null || order.getUser() == null || !order.getUser().getEmail().equals(email)) {
            return "redirect:/user/orders";
        }

        model.addAttribute("user", user);
        model.addAttribute("order", order);

        return "user/order-detail";
    }
}
