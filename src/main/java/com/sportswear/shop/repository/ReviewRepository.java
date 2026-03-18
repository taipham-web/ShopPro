package com.sportswear.shop.repository;

import com.sportswear.shop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByProductIdAndApprovedTrue(Long productId);

    List<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);
    
    List<Review> findByApprovedFalse();
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    Double getAverageRating(@Param("productId") Long productId);

    @Query("SELECT r.product.id, AVG(r.rating), COUNT(r) FROM Review r WHERE r.approved = true GROUP BY r.product.id")
    List<Object[]> getAverageRatingsByProduct();
}
