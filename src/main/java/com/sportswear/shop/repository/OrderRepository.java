package com.sportswear.shop.repository;

import com.sportswear.shop.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderCode(String orderCode);
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByUserEmailOrderByCreatedAtDesc(String email);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED' AND o.createdAt BETWEEN :start AND :end")
    BigDecimal calculateRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    Long countOrdersByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Lấy doanh thu theo tháng trong năm
    @Query("SELECT MONTH(o.createdAt), COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.status = 'DELIVERED' AND YEAR(o.createdAt) = :year " +
           "GROUP BY MONTH(o.createdAt) ORDER BY MONTH(o.createdAt)")
    List<Object[]> getMonthlyRevenue(@Param("year") int year);
    
    // Lấy doanh thu theo danh mục
    @Query("SELECT c.name, COALESCE(SUM(od.price * od.quantity), 0) FROM Order o " +
           "JOIN o.orderDetails od " +
           "JOIN od.product p " +
           "JOIN p.category c " +
           "WHERE o.status = 'DELIVERED' " +
           "GROUP BY c.id, c.name ORDER BY SUM(od.price * od.quantity) DESC")
    List<Object[]> getRevenueByCategory();
}
