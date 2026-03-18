package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.user.CartController;
import com.sportswear.shop.controller.user.CheckoutController;
import com.sportswear.shop.dto.CartItemDTO;
import com.sportswear.shop.entity.Order;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.IOrderService;
import com.sportswear.shop.service.impl.CustomOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({CartController.class, CheckoutController.class})
@Import(SecurityConfig.class)
class CartCheckoutWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private IOrderService orderService;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void anonymousCheckout_shouldRedirectLogin() throws Exception {
        mockMvc.perform(get("/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void addToCart_shouldReturnSuccessJson() throws Exception {
        when(cartService.getCartCount("user@sportwear.vn")).thenReturn(3);

        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", "10")
                        .param("quantity", "1")
                        .param("size", "M"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"success\": true")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"count\": 3")));

        verify(cartService).addToCart("user@sportwear.vn", 10L, 1, "M");
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void updateCart_shouldReturnTotalsWithShipping() throws Exception {
        CartItemDTO item = new CartItemDTO(1L, 10L, "Shoes", "/img.png", new BigDecimal("200000"), 2, "M");
        when(cartService.getCart(eq("user@sportwear.vn"))).thenReturn(List.of(item));
        when(cartService.getCartTotal(eq("user@sportwear.vn"))).thenReturn(new BigDecimal("400000"));
        when(cartService.getCartCount(eq("user@sportwear.vn"))).thenReturn(2);

        mockMvc.perform(post("/cart/update")
                        .with(csrf())
                        .param("cartItemId", "1")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"success\":true")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"grandTotal\":430000")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"freeShipping\":false")));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void updateCartQuantityZero_shouldRemoveItem() throws Exception {
        when(cartService.getCart("user@sportwear.vn")).thenReturn(List.of());
        when(cartService.getCartTotal("user@sportwear.vn")).thenReturn(BigDecimal.ZERO);
        when(cartService.getCartCount("user@sportwear.vn")).thenReturn(0);

        mockMvc.perform(post("/cart/update")
                        .with(csrf())
                        .param("cartItemId", "1")
                        .param("quantity", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"count\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"grandTotal\":30000")));

        verify(cartService).updateQuantity("user@sportwear.vn", 1L, 0);
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void checkoutCod_shouldCreateOrderAndClearCart() throws Exception {
        User user = new User();
        user.setEmail("user@sportwear.vn");

        CartItemDTO cartItem = new CartItemDTO(1L, 10L, "Shoes", "/img.png", new BigDecimal("200000"), 2, "M");

        Product product = new Product();
        product.setId(10L);
        product.setName("Shoes");

        Order saved = new Order();
        saved.setOrderCode("ORD123456");
        saved.setTotalAmount(new BigDecimal("430000"));

        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(cartService.getCart("user@sportwear.vn")).thenReturn(List.of(cartItem));
        when(cartService.getCartTotal("user@sportwear.vn")).thenReturn(new BigDecimal("400000"));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderService.save(any(Order.class))).thenReturn(saved);

        mockMvc.perform(post("/checkout/process")
                        .with(csrf())
                        .param("shippingName", "Test User")
                        .param("shippingPhone", "0900000000")
                        .param("shippingAddress", "123 Test Street")
                        .param("paymentMethod", "COD"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/checkout-success"));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).save(captor.capture());
        Order created = captor.getValue();

        assertThat(created.getShippingFee()).isEqualByComparingTo("30000");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("430000");
        assertThat(created.getPaymentMethod()).isEqualTo(Order.PaymentMethod.COD);
        assertThat(created.isPaid()).isFalse();
        assertThat(created.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        verify(cartService).clearCart("user@sportwear.vn");
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void checkoutWithEmptyCart_shouldRedirectToCart() throws Exception {
        User user = new User();
        user.setEmail("user@sportwear.vn");
        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(cartService.getCart("user@sportwear.vn")).thenReturn(List.of());

        mockMvc.perform(post("/checkout/process")
                        .with(csrf())
                        .param("shippingName", "Test User")
                        .param("shippingPhone", "0900000000")
                        .param("shippingAddress", "123 Test Street")
                        .param("paymentMethod", "COD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }
}
