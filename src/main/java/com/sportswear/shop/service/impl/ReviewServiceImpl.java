package com.sportswear.shop.service.impl;

import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.Review;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.ReviewRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.IReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewServiceImpl implements IReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Review addReview(Long productId, Long userId, int rating, String comment) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(Math.max(1, Math.min(5, rating)));
        review.setComment(comment != null ? comment.trim() : "");
        review.setApproved(true); // auto-approve

        return reviewRepository.save(review);
    }

    @Override
    public List<Review> getApprovedReviews(Long productId) {
        return reviewRepository.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId);
    }

    @Override
    public boolean hasReviewed(Long userId, Long productId) {
        return reviewRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public double getAverageRating(Long productId) {
        Double avg = reviewRepository.getAverageRating(productId);
        return avg != null ? avg : 0.0;
    }
}
