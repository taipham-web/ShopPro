package com.sportswear.shop.service.impl;

import com.sportswear.shop.dto.ProductDTO;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements IProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product save(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setSalePrice(productDTO.getSalePrice());
        product.setQuantity(productDTO.getQuantity());
        product.setImage(productDTO.getImage());
        return productRepository.save(product);
    }

    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public Page<Product> findByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    @Override
    public Page<Product> search(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable);
    }

    @Override
    public List<Product> findLatestProducts(int limit) {
        return productRepository.findLatestProducts(PageRequest.of(0, limit));
    }

    @Override
    public Page<Product> findWithFilters(String keyword, Long categoryId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         Pageable pageable) {
        return productRepository.findWithFilters(keyword, categoryId, minPrice, maxPrice, pageable);
    }
}
