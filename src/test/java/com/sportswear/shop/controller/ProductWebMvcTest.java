package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.user.ProductController;
import com.sportswear.shop.entity.Category;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.CategoryRepository;
import com.sportswear.shop.repository.ReviewRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.repository.WishlistRepository;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.IProductService;
import com.sportswear.shop.service.IReviewService;
import com.sportswear.shop.service.impl.CustomOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProductService productService;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private WishlistRepository wishlistRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private IReviewService reviewService;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

        @MockitoBean
        private UserDetailsService userDetailsService;

    @Test
    void productListPagination_shouldRenderCorrectView() throws Exception {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("Product 1");

        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("Product 2");

        Category c1 = new Category();
        c1.setId(10L);
        c1.setName("Shoes");

        when(productService.findWithFilters(eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 12), 25));
        when(categoryRepository.findAll()).thenReturn(List.of(c1));
        when(reviewRepository.getAverageRatingsByProduct()).thenReturn(List.of());

        mockMvc.perform(get("/products").param("page", "0").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/product-list"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalItems", 25L));
    }

    @Test
    void productListFilterByCategory_shouldRenderFilteredResult() throws Exception {
        Product p1 = new Product();
        p1.setId(1L);

        Category c1 = new Category();
        c1.setId(10L);
        c1.setName("Shoes");

        when(productService.findWithFilters(eq(null), eq(10L), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(p1), PageRequest.of(0, 12), 1));
        when(categoryRepository.findAll()).thenReturn(List.of(c1));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(c1));
        when(reviewRepository.getAverageRatingsByProduct()).thenReturn(List.of());

        mockMvc.perform(get("/products").param("categoryId", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/product-list"))
                .andExpect(model().attribute("categoryId", 10L));
    }

    @Test
    void productListFilterByPrice_shouldRenderFilteredResult() throws Exception {
        when(productService.findWithFilters(eq(null), eq(null), eq(new BigDecimal("100000")), eq(new BigDecimal("500000")), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(reviewRepository.getAverageRatingsByProduct()).thenReturn(List.of());

        mockMvc.perform(get("/products")
                        .param("minPrice", "100000")
                        .param("maxPrice", "500000"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/product-list"))
                .andExpect(model().attribute("minPrice", new BigDecimal("100000")))
                .andExpect(model().attribute("maxPrice", new BigDecimal("500000")));
    }

    @Test
    void productListSearchKeyword_shouldRenderSearchResult() throws Exception {
        when(productService.findWithFilters(eq("shoe"), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(reviewRepository.getAverageRatingsByProduct()).thenReturn(List.of());

        mockMvc.perform(get("/products").param("keyword", "shoe"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/product-list"))
                .andExpect(model().attribute("keyword", "shoe"));
    }

    @Test
    void productListSort_shouldRenderSortedResult() throws Exception {
        when(productService.findWithFilters(eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(reviewRepository.getAverageRatingsByProduct()).thenReturn(List.of());

        mockMvc.perform(get("/products")
                        .param("sortBy", "name")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/product-list"))
                .andExpect(model().attribute("sortBy", "name"))
                .andExpect(model().attribute("sortDir", "asc"));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void productDetail_shouldRenderDetailPage() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Product 1");
                Category category = new Category();
                category.setId(10L);
                product.setCategory(category);

        User user = new User();
        user.setId(11L);
        user.setEmail("user@sportwear.vn");

        when(productService.findById(1L)).thenReturn(Optional.of(product));
        when(productService.findLatestProducts(4)).thenReturn(List.of());
        when(userRepository.findByEmail("user@sportwear.vn")).thenReturn(Optional.of(user));
        when(wishlistRepository.existsByUserAndProduct(user, product)).thenReturn(false);
        when(reviewService.hasReviewed(11L, 1L)).thenReturn(false);
        when(reviewService.getApprovedReviews(1L)).thenReturn(List.of());
        when(reviewService.getAverageRating(1L)).thenReturn(0.0);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/product-detail"));
    }

    @Test
    void productsByCategoryShortcut_shouldRedirectToFilterUrl() throws Exception {
        mockMvc.perform(get("/products/category/5").param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products?categoryId=5&page=2"));
    }

    @Test
    void searchShortcut_shouldRedirectToFilterUrl() throws Exception {
        mockMvc.perform(get("/products/search").param("keyword", "nike").param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products?keyword=nike&page=1"));
    }
}
