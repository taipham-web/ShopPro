package com.sportswear.shop.service;

import com.sportswear.shop.dto.CartItemDTO;
import com.sportswear.shop.entity.CartItem;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.CartItemRepository;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void getCart_shouldUseSalePriceIfPresent() {
        User user = new User();
        user.setEmail("u@sportwear.vn");

        Product product = new Product();
        product.setId(1L);
        product.setName("Shoes");
        product.setPrice(new BigDecimal("100000"));
        product.setSalePrice(new BigDecimal("80000"));
        product.setImage("shoe.jpg");

        CartItem item = new CartItem();
        item.setId(5L);
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(2);
        item.setSize("M");

        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUser(user)).thenReturn(List.of(item));

        List<CartItemDTO> result = cartService.getCart("u@sportwear.vn");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrice()).isEqualByComparingTo("80000");
        assertThat(result.get(0).getSubtotal()).isEqualByComparingTo("160000");
    }

    @Test
    void addToCart_whenProductNotFound_shouldDoNothing() {
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        cartService.addToCart("u@sportwear.vn", 10L, 1, "L");

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addToCart_whenExistingItem_shouldCapQuantityByStock() {
        User user = new User();
        user.setEmail("u@sportwear.vn");

        Product product = new Product();
        product.setId(1L);
        product.setQuantity(5);

        CartItem existing = new CartItem();
        existing.setQuantity(4);
        existing.setProduct(product);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserProductAndSize(user, product, "M")).thenReturn(Optional.of(existing));

        cartService.addToCart("u@sportwear.vn", 1L, 3, "M");

        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addToCart_whenNewItem_shouldCreateWithCappedQuantity() {
        User user = new User();
        user.setEmail("u@sportwear.vn");

        Product product = new Product();
        product.setId(1L);
        product.setQuantity(2);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUserProductAndSize(user, product, "S")).thenReturn(Optional.empty());

        cartService.addToCart("u@sportwear.vn", 1L, 5, "S");

        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void updateQuantity_whenZero_shouldRemoveItem() {
        CartItem item = new CartItem();
        item.setId(7L);
        when(cartItemRepository.findById(7L)).thenReturn(Optional.of(item));

        cartService.updateQuantity("u@sportwear.vn", 7L, 0);

        verify(cartItemRepository).delete(item);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateQuantity_whenPositive_shouldSaveNewQuantity() {
        CartItem item = new CartItem();
        item.setId(7L);
        item.setQuantity(1);
        when(cartItemRepository.findById(7L)).thenReturn(Optional.of(item));

        cartService.updateQuantity("u@sportwear.vn", 7L, 3);

        assertThat(item.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(item);
    }

    @Test
    void getCartTotalAndCount_shouldAggregate() {
        User user = new User();
        user.setEmail("u@sportwear.vn");

        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("A");
        p1.setPrice(new BigDecimal("100000"));

        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("B");
        p2.setPrice(new BigDecimal("50000"));

        CartItem i1 = new CartItem();
        i1.setId(1L);
        i1.setProduct(p1);
        i1.setQuantity(2);

        CartItem i2 = new CartItem();
        i2.setId(2L);
        i2.setProduct(p2);
        i2.setQuantity(1);

        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUser(user)).thenReturn(List.of(i1, i2));

        assertThat(cartService.getCartTotal("u@sportwear.vn")).isEqualByComparingTo("250000");
        assertThat(cartService.getCartCount("u@sportwear.vn")).isEqualTo(3);
    }

    @Test
    void clearCart_shouldDeleteAllItemsOfUser() {
        User user = new User();
        user.setEmail("u@sportwear.vn");
        List<CartItem> items = List.of(new CartItem(), new CartItem());

        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUser(user)).thenReturn(items);

        cartService.clearCart("u@sportwear.vn");

        verify(cartItemRepository).deleteAll(eq(items));
    }
}
