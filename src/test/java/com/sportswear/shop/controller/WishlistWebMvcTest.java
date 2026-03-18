package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.user.WishlistController;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.repository.WishlistRepository;
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

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
@Import(SecurityConfig.class)
class WishlistWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistRepository wishlistRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void addWishlistNewProduct_shouldRedirectProductDetail() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@sportwear.vn");

        Product product = new Product();
        product.setId(10L);

        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserAndProduct(user, product)).thenReturn(false);

        mockMvc.perform(post("/user/wishlist/add/10").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/10"));

        verify(wishlistRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void addWishlistDuplicate_shouldNotSave() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@sportwear.vn");

        Product product = new Product();
        product.setId(10L);

        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserAndProduct(user, product)).thenReturn(true);

        mockMvc.perform(post("/user/wishlist/add/10").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/10"));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void toggleWishlistTwice_shouldReturnAddedThenRemoved() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@sportwear.vn");

        Product product = new Product();
        product.setId(10L);

        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserAndProduct(user, product)).thenReturn(false, true);

        mockMvc.perform(post("/user/wishlist/toggle/10").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("added"));

        mockMvc.perform(post("/user/wishlist/toggle/10").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("removed"));
    }
}
