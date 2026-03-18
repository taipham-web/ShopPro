package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.user.ReviewController;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.IReviewService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IReviewService reviewService;

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
    void submitReviewSuccess_shouldRedirectDetail() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@sportwear.vn");

        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(reviewService.hasReviewed(1L, 10L)).thenReturn(false);

        mockMvc.perform(post("/products/10/review")
                        .with(csrf())
                        .param("rating", "5")
                        .param("comment", "Great product"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/10"));

        verify(reviewService).addReview(10L, 1L, 5, "Great product");
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void duplicateReview_shouldRedirectWithError() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@sportwear.vn");

        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(reviewService.hasReviewed(1L, 10L)).thenReturn(true);

        mockMvc.perform(post("/products/10/review")
                        .with(csrf())
                        .param("rating", "5")
                        .param("comment", "Again"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/10"));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void blankComment_shouldRedirectWithValidationError() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@sportwear.vn");

        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(reviewService.hasReviewed(1L, 10L)).thenReturn(false);

        mockMvc.perform(post("/products/10/review")
                        .with(csrf())
                        .param("rating", "5")
                        .param("comment", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/10"));
    }
}
