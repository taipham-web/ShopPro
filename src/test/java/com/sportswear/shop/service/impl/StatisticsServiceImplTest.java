package com.sportswear.shop.service.impl;

import com.sportswear.shop.repository.OrderRepository;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Test
    void getTotalRevenue_whenNull_shouldReturnZero() {
        when(orderRepository.calculateRevenue(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(null);

        BigDecimal revenue = statisticsService.getTotalRevenue(LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(revenue).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getDashboardStatistics_shouldContainAllMetrics() {
        when(orderRepository.calculateRevenue(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("1230000"));
        when(orderRepository.countOrdersByDateRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(15L);
        when(productRepository.count()).thenReturn(70L);
        when(userRepository.count()).thenReturn(99L);

        Map<String, Object> stats = statisticsService.getDashboardStatistics();

        assertThat(stats.get("totalRevenue")).isEqualTo(new BigDecimal("1230000"));
        assertThat(stats.get("totalOrders")).isEqualTo(15L);
        assertThat(stats.get("totalProducts")).isEqualTo(70L);
        assertThat(stats.get("totalUsers")).isEqualTo(99L);
    }
}
