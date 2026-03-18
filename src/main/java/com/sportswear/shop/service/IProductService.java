package com.sportswear.shop.service;

import com.sportswear.shop.dto.ProductDTO;
import com.sportswear.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Interface service sản phẩm
 */
public interface IProductService {
    
    List<Product> findAll();
    
    Page<Product> findAll(Pageable pageable);
    
    Optional<Product> findById(Long id);
    
    Product save(Product product);
    
    Product save(ProductDTO productDTO);
    
    void deleteById(Long id);
    
    Page<Product> findByCategory(Long categoryId, Pageable pageable);
    
    Page<Product> search(String keyword, Pageable pageable);
    
    List<Product> findLatestProducts(int limit);

    Page<Product> findWithFilters(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}

