package com.sportswear.shop.service.impl;

import com.sportswear.shop.entity.Order;
import com.sportswear.shop.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void updateStatus_whenOrderExists_shouldSaveWithNewStatus() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.updateStatus(1L, Order.OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatus_whenOrderNotFound_shouldNotSave() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        orderService.updateStatus(99L, Order.OrderStatus.DELIVERED);

        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any(Order.class));
    }

    @Test
    void basicDelegationMethods_shouldDelegateToRepository() {
        Order order = new Order();
        order.setOrderCode("ORD-001");

        PageRequest pageable = PageRequest.of(0, 10);

        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.findByOrderCode("ORD-001")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderRepository.findByUserId(11L, pageable)).thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findByStatus(Order.OrderStatus.PENDING)).thenReturn(List.of(order));

        assertThat(orderService.findAll()).hasSize(1);
        assertThat(orderService.findAll(pageable).getContent()).hasSize(1);
        assertThat(orderService.findById(1L)).contains(order);
        assertThat(orderService.findByOrderCode("ORD-001")).contains(order);
        assertThat(orderService.save(order)).isSameAs(order);
        assertThat(orderService.findByUserId(11L, pageable).getContent()).hasSize(1);
        assertThat(orderService.findByStatus(Order.OrderStatus.PENDING)).hasSize(1);
    }
}
