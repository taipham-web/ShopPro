package com.sportswear.shop.service;

import com.sportswear.shop.entity.Review;

import java.util.List;

public interface IReviewService {

    /** Lưu đánh giá của người dùng cho sản phẩm. */
    Review addReview(Long productId, Long userId, int rating, String comment);

    /** Lấy danh sách đánh giá đã duyệt của sản phẩm (mới nhất trước). */
    List<Review> getApprovedReviews(Long productId);

    /** Kiểm tra người dùng đã đánh giá sản phẩm chưa. */
    boolean hasReviewed(Long userId, Long productId);

    /** Điểm trung bình (trả về 0.0 nếu chưa có đánh giá). */
    double getAverageRating(Long productId);
}
