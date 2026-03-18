package com.sportswear.shop.repository;

import com.sportswear.shop.entity.CartItem;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);

    @Query("SELECT c FROM CartItem c WHERE c.user = :user AND c.product = :product AND (c.size = :size OR (c.size IS NULL AND :size IS NULL))")
    Optional<CartItem> findByUserProductAndSize(@Param("user") User user, @Param("product") Product product, @Param("size") String size);
}
