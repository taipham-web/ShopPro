package com.sportswear.shop.controller.admin;

import com.sportswear.shop.entity.Order;
import com.sportswear.shop.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            Model model) {
        
        Page<Order> orders;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findAll(pageRequest);
            // Filter by status
            var filteredOrders = orders.getContent().stream()
                .filter(o -> o.getStatus().name().equals(status))
                .toList();
            model.addAttribute("orders", filteredOrders);
        } else {
            orders = orderRepository.findAll(pageRequest);
            model.addAttribute("orders", orders.getContent());
        }
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("totalItems", orders.getTotalElements());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", Order.OrderStatus.values());
        
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()) {
            return "redirect:/admin/orders";
        }
        
        model.addAttribute("order", order.get());
        model.addAttribute("statuses", Order.OrderStatus.values());
        
        return "admin/orders/detail";
    }

    @PostMapping("/{id}/status")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(Order.OrderStatus.valueOf(status));
            
            // Nếu trạng thái là DELIVERED, đánh dấu đã thanh toán
            if (status.equals("DELIVERED")) {
                order.setPaid(true);
            }
            
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng!");
        }
        
        return "redirect:/admin/orders/" + id;
    }
}
