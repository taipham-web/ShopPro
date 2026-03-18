package com.sportswear.shop.repository;

import com.sportswear.shop.entity.Wishlist;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    List<Wishlist> findByUserOrderByCreatedAtDesc(User user);
    
    List<Wishlist> findByUserId(Long userId);
    
    Optional<Wishlist> findByUserAndProduct(User user, Product product);
    
    boolean existsByUserAndProduct(User user, Product product);
    
    void deleteByUserAndProduct(User user, Product product);
    
    long countByUser(User user);
}
