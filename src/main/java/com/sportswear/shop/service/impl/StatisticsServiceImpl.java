package com.sportswear.shop.service.impl;

import com.sportswear.shop.repository.OrderRepository;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service xử lý thống kê doanh thu
 */
@Service
public class StatisticsServiceImpl {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public BigDecimal getTotalRevenue(LocalDateTime start, LocalDateTime end) {
        BigDecimal revenue = orderRepository.calculateRevenue(start, end);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public Long getTotalOrders(LocalDateTime start, LocalDateTime end) {
        return orderRepository.countOrdersByDateRange(start, end);
    }

    public Long getTotalProducts() {
        return productRepository.count();
    }

    public Long getTotalUsers() {
        return userRepository.count();
    }

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime now = LocalDateTime.now();
        
        stats.put("totalRevenue", getTotalRevenue(startOfMonth, now));
        stats.put("totalOrders", getTotalOrders(startOfMonth, now));
        stats.put("totalProducts", getTotalProducts());
        stats.put("totalUsers", getTotalUsers());
        
        return stats;
    }
}
