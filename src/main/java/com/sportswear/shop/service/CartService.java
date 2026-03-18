package com.sportswear.shop.service;

import com.sportswear.shop.dto.CartItemDTO;
import com.sportswear.shop.entity.CartItem;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.CartItemRepository;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    public List<CartItemDTO> getCart(String username) {
        User user = getUser(username);
        return cartItemRepository.findByUser(user).stream()
                .map(item -> {
                    Product p = item.getProduct();
                    BigDecimal price = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
                    return new CartItemDTO(item.getId(), p.getId(), p.getName(), p.getImage(), price, item.getQuantity(), item.getSize());
                })
                .collect(Collectors.toList());
    }

    public void addToCart(String username, Long productId, int quantity, String size) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        User user = getUser(username);
        Optional<CartItem> existing = cartItemRepository.findByUserProductAndSize(user, product, size);

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + quantity;
            item.setQuantity(Math.min(newQty, product.getQuantity()));
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setSize(size);
            item.setQuantity(Math.min(quantity, product.getQuantity()));
            cartItemRepository.save(item);
        }
    }

    public void updateQuantity(String username, Long cartItemId, int quantity) {
        if (quantity <= 0) {
            removeFromCart(username, cartItemId);
            return;
        }
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        });
    }

    public void removeFromCart(String username, Long cartItemId) {
        cartItemRepository.findById(cartItemId).ifPresent(cartItemRepository::delete);
    }

    public BigDecimal getCartTotal(String username) {
        return getCart(username).stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getCartCount(String username) {
        return getCart(username).stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();
    }

    /**
     * Xóa toàn bộ giỏ hàng của user (dùng sau khi đặt hàng thành công)
     */
    public void clearCart(String username) {
        User user = getUser(username);
        List<CartItem> items = cartItemRepository.findByUser(user);
        cartItemRepository.deleteAll(items);
    }
}
