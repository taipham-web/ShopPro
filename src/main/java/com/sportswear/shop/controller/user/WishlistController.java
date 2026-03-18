package com.sportswear.shop.controller.user;

import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.entity.Wishlist;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.transaction.Transactional;
import java.util.List;

@Controller
@RequestMapping("/user/wishlist")
public class WishlistController {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User getCurrentUser(UserDetails userDetails) {
        // Đăng nhập bằng email nên getUsername() trả về email
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public String viewWishlist(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getCurrentUser(userDetails);
        
        List<Wishlist> wishlistItems = wishlistRepository.findByUserOrderByCreatedAtDesc(user);
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("totalItems", wishlistItems.size());
        
        return "user/wishlist";
    }

    @PostMapping("/add/{productId}")
    public String addToWishlist(@PathVariable Long productId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(userDetails);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check if already in wishlist
        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            redirectAttributes.addFlashAttribute("warningMessage", "Sản phẩm đã có trong danh sách yêu thích!");
        } else {
            Wishlist wishlist = new Wishlist(user, product);
            wishlistRepository.save(wishlist);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm vào danh sách yêu thích!");
        }
        
        return "redirect:/products/" + productId;
    }

    @PostMapping("/remove/{wishlistId}")
    @Transactional
    public String removeFromWishlist(@PathVariable Long wishlistId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(userDetails);
        
        Wishlist wishlist = wishlistRepository.findById(wishlistId).orElse(null);
        
        if (wishlist != null && wishlist.getUser().getId().equals(user.getId())) {
            wishlistRepository.delete(wishlist);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khỏi danh sách yêu thích!");
        }
        
        return "redirect:/user/wishlist";
    }

    @PostMapping("/toggle/{productId}")
    @Transactional
    @ResponseBody
    public String toggleWishlist(@PathVariable Long productId,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            wishlistRepository.deleteByUserAndProduct(user, product);
            return "removed";
        } else {
            Wishlist wishlist = new Wishlist(user, product);
            wishlistRepository.save(wishlist);
            return "added";
        }
    }
}
