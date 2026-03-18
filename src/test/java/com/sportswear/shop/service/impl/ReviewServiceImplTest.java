package com.sportswear.shop.service.impl;

import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.Review;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.ReviewRepository;
import com.sportswear.shop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void addReview_shouldClampRatingTrimCommentAndApprove() {
        Product product = new Product();
        User user = new User();

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review saved = reviewService.addReview(10L, 20L, 10, "  Great  ");

        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("Great");
        assertThat(saved.isApproved()).isTrue();
    }

    @Test
    void addReview_whenProductNotFound_shouldThrow() {
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addReview(10L, 20L, 5, "ok"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy sản phẩm");
    }

    @Test
    void getAverageRating_whenRepositoryReturnsNull_shouldReturnZero() {
        when(reviewRepository.getAverageRating(10L)).thenReturn(null);

        assertThat(reviewService.getAverageRating(10L)).isEqualTo(0.0);
    }

    @Test
    void getApprovedAndHasReviewed_shouldDelegateToRepository() {
        when(reviewRepository.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(10L)).thenReturn(List.of(new Review()));
        when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);

        assertThat(reviewService.getApprovedReviews(10L)).hasSize(1);
        assertThat(reviewService.hasReviewed(1L, 10L)).isTrue();
    }
}
