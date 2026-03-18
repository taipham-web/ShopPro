package com.sportswear.shop.service;

import com.sportswear.shop.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interface service đơn hàng
 */
public interface IOrderService {
    
    List<Order> findAll();
    
    Page<Order> findAll(Pageable pageable);
    
    Optional<Order> findById(Long id);
    
    Optional<Order> findByOrderCode(String orderCode);
    
    Order save(Order order);
    
    void updateStatus(Long orderId, Order.OrderStatus status);
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    List<Order> findByStatus(Order.OrderStatus status);
}
