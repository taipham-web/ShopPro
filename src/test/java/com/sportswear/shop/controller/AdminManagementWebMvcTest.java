package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.admin.AdminOrderController;
import com.sportswear.shop.controller.admin.AdminProductController;
import com.sportswear.shop.controller.admin.AdminUserController;
import com.sportswear.shop.entity.Order;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.BrandRepository;
import com.sportswear.shop.repository.CategoryRepository;
import com.sportswear.shop.repository.OrderRepository;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.impl.CustomOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminOrderController.class, AdminUserController.class, AdminProductController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.upload.dir=target/test-uploads")
class AdminManagementWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private BrandRepository brandRepository;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private CartService cartService;

    @Test
    @WithMockUser(username = "admin@sportwear.vn", roles = {"ADMIN"})
    void adminUpdateOrderDelivered_shouldSetPaidTrue() throws Exception {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaid(false);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(post("/admin/orders/1/status")
                        .with(csrf())
                        .param("status", "DELIVERED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders/1"));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        assertThat(saved.isPaid()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin@sportwear.vn", roles = {"ADMIN"})
    void adminToggleUserStatus_shouldFlipEnabled() throws Exception {
        User user = new User();
        user.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/admin/users/toggle-status/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isFalse();
    }

    @Test
    @WithMockUser(username = "admin@sportwear.vn", roles = {"ADMIN"})
    void adminToggleProductActive_shouldFlipActive() throws Exception {
        Product product = new Product();
        product.setActive(true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        mockMvc.perform(post("/admin/products/toggle-active/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @WithMockUser(username = "admin@sportwear.vn", roles = {"ADMIN"})
    void adminToggleProductFeatured_shouldFlipFeatured() throws Exception {
        Product product = new Product();
        product.setFeatured(false);

        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        mockMvc.perform(post("/admin/products/toggle-featured/2").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().isFeatured()).isTrue();
    }

        @Test
        @WithMockUser(username = "admin@sportwear.vn", roles = {"ADMIN"})
        void adminCreateProductWithImages_shouldRedirectProductList() throws Exception {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile mainImage = new MockMultipartFile(
            "imageFile", "main.jpg", "image/jpeg", "fake-image".getBytes());
        MockMultipartFile extraImage = new MockMultipartFile(
            "imageFiles", "extra.jpg", "image/jpeg", "fake-image-2".getBytes());

        MockMultipartHttpServletRequestBuilder request = multipart("/admin/products/create")
            .file(mainImage)
            .file(extraImage)
            .param("name", "New Product")
            .param("description", "Desc")
            .param("price", "200000")
            .param("quantity", "10")
            .param("sizes", "M", "L")
            .with(csrf());

        mockMvc.perform(request)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/products"));

        verify(productRepository).save(any(Product.class));
        }

    @Test
    @WithMockUser(username = "admin@sportwear.vn", roles = {"ADMIN"})
    void adminUpdateOrderInvalidStatus_shouldReturnServerError() throws Exception {
        Order order = new Order();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> mockMvc.perform(post("/admin/orders/1/status")
                .with(csrf())
                .param("status", "NOT_A_STATUS")))
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
