package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.user.UserOrderController;
import com.sportswear.shop.entity.Order;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.OrderRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.impl.CustomOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserOrderController.class)
@Import(SecurityConfig.class)
class UserOrderWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void myOrders_shouldShowOnlyCurrentUserOrders() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@sportwear.vn");

        Order pending = new Order();
        pending.setStatus(Order.OrderStatus.PENDING);
        pending.setUser(user);

        Order delivered = new Order();
        delivered.setStatus(Order.OrderStatus.DELIVERED);
        delivered.setUser(user);

        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserEmailOrderByCreatedAtDesc("user@sportwear.vn"))
                .thenReturn(List.of(pending, delivered));

        mockMvc.perform(get("/user/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/my-orders"))
                .andExpect(model().attribute("totalOrders", 2))
                .andExpect(model().attribute("pendingCount", 1L))
                .andExpect(model().attribute("deliveredCount", 1L));
    }

    @Test
    @WithMockUser(username = "userA@sportwear.vn", roles = {"USER"})
    void userAAccessUserBOrder_shouldRedirectToMyOrders() throws Exception {
        User userA = new User();
        userA.setId(1L);
        userA.setEmail("userA@sportwear.vn");

        User userB = new User();
        userB.setId(2L);
        userB.setEmail("userB@sportwear.vn");

        Order orderB = new Order();
        orderB.setUser(userB);

        when(userRepository.findByEmail("userA@sportwear.vn")).thenReturn(Optional.of(userA));
        when(orderRepository.findByOrderCode("ORD-B")).thenReturn(Optional.of(orderB));

        mockMvc.perform(get("/user/orders/ORD-B"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/orders"));
    }
}
