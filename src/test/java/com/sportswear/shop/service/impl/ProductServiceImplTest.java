package com.sportswear.shop.service.impl;

import com.sportswear.shop.dto.ProductDTO;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void saveProductDto_shouldMapFieldsBeforeSaving() {
        ProductDTO dto = new ProductDTO();
        dto.setName("Air Shoes");
        dto.setDescription("Desc");
        dto.setPrice(new BigDecimal("500000"));
        dto.setSalePrice(new BigDecimal("450000"));
        dto.setQuantity(20);
        dto.setImage("air.jpg");

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product saved = productService.save(dto);

        assertThat(saved.getName()).isEqualTo("Air Shoes");
        assertThat(saved.getPrice()).isEqualByComparingTo("500000");
        assertThat(saved.getSalePrice()).isEqualByComparingTo("450000");
        assertThat(saved.getQuantity()).isEqualTo(20);
    }

    @Test
    void findWithFilters_shouldDelegateToRepository() {
        PageRequest pageable = PageRequest.of(0, 12);
        when(productRepository.findWithFilters(eq("shoe"), eq(1L), eq(new BigDecimal("100000")), eq(new BigDecimal("500000")), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(new Product())));

        assertThat(productService.findWithFilters("shoe", 1L, new BigDecimal("100000"), new BigDecimal("500000"), pageable)
                .getContent()).hasSize(1);
    }

    @Test
    void findLatestProducts_shouldUsePageRequestWithLimit() {
        when(productRepository.findLatestProducts(any())).thenReturn(List.of(new Product(), new Product()));

        List<Product> latest = productService.findLatestProducts(2);

        assertThat(latest).hasSize(2);
        ArgumentCaptor<org.springframework.data.domain.Pageable> captor = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(productRepository).findLatestProducts(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void basicDelegationMethods_shouldDelegateToRepository() {
        Product product = new Product();
        product.setId(9L);

        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product)));
        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productRepository.findByCategoryId(1L, pageable)).thenReturn(new PageImpl<>(List.of(product)));
        when(productRepository.searchProducts("shoe", pageable)).thenReturn(new PageImpl<>(List.of(product)));

        assertThat(productService.findAll()).hasSize(1);
        assertThat(productService.findAll(pageable).getContent()).hasSize(1);
        assertThat(productService.findById(9L)).contains(product);
        assertThat(productService.save(product)).isSameAs(product);
        assertThat(productService.findByCategory(1L, pageable).getContent()).hasSize(1);
        assertThat(productService.search("shoe", pageable).getContent()).hasSize(1);

        productService.deleteById(9L);
        verify(productRepository).deleteById(9L);
    }
}
