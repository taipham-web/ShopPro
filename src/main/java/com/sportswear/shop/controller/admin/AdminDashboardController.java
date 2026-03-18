package com.sportswear.shop.controller.admin;

import com.sportswear.shop.entity.Order;
import com.sportswear.shop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller thống kê trang quản trị
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        // Thống kê tổng quan
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();
        long totalCategories = categoryRepository.count();
        long totalBrands = brandRepository.count();

        // Đơn hàng theo trạng thái
        long pendingOrders = orderRepository.findByStatus(Order.OrderStatus.PENDING).size();
        long confirmedOrders = orderRepository.findByStatus(Order.OrderStatus.CONFIRMED).size();
        long shippingOrders = orderRepository.findByStatus(Order.OrderStatus.SHIPPING).size();
        long deliveredOrders = orderRepository.findByStatus(Order.OrderStatus.DELIVERED).size();
        long cancelledOrders = orderRepository.findByStatus(Order.OrderStatus.CANCELLED).size();

        // Doanh thu tháng này
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal monthlyRevenue = orderRepository.calculateRevenue(startOfMonth, now);
        if (monthlyRevenue == null) monthlyRevenue = BigDecimal.ZERO;

        // Đơn hàng gần đây
        var recentOrders = orderRepository.findAll(
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        // Dữ liệu doanh thu theo tháng (cho biểu đồ)
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> monthlyRevenueData = orderRepository.getMonthlyRevenue(currentYear);
        
        // Khởi tạo mảng 12 tháng với giá trị 0
        List<BigDecimal> revenueByMonth = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            revenueByMonth.add(BigDecimal.ZERO);
        }
        
        // Điền dữ liệu thực vào
        for (Object[] row : monthlyRevenueData) {
            int month = ((Number) row[0]).intValue();
            BigDecimal revenue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            revenueByMonth.set(month - 1, revenue);
        }
        
        // Dữ liệu doanh thu theo danh mục (cho biểu đồ tròn)
        List<Object[]> categoryRevenueData = orderRepository.getRevenueByCategory();
        List<String> categoryNames = new ArrayList<>();
        List<BigDecimal> categoryRevenues = new ArrayList<>();
        
        for (Object[] row : categoryRevenueData) {
            categoryNames.add((String) row[0]);
            categoryRevenues.add(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }
        
        // Nếu không có dữ liệu danh mục, thêm placeholder
        if (categoryNames.isEmpty()) {
            categoryNames.add("Chưa có dữ liệu");
            categoryRevenues.add(BigDecimal.ZERO);
        }

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalBrands", totalBrands);
        model.addAttribute("totalRevenue", monthlyRevenue);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("confirmedOrders", confirmedOrders);
        model.addAttribute("shippingOrders", shippingOrders);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("recentOrders", recentOrders);
        
        // Dữ liệu biểu đồ
        model.addAttribute("revenueByMonth", revenueByMonth);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("categoryRevenues", categoryRevenues);
        model.addAttribute("currentYear", currentYear);

        return "admin/dashboard";
    }
}
