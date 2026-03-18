package com.sportswear.shop.controller.user;

import com.sportswear.shop.entity.Category;
import com.sportswear.shop.entity.Product;
import com.sportswear.shop.entity.Review;
import com.sportswear.shop.entity.Wishlist;
import com.sportswear.shop.repository.CategoryRepository;
import com.sportswear.shop.repository.ReviewRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.repository.WishlistRepository;
import com.sportswear.shop.service.IProductService;
import com.sportswear.shop.service.IReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller xem, tìm kiếm sản phẩm
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private IProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    private Set<Long> getWishlistProductIds(UserDetails userDetails) {
        if (userDetails == null) return Collections.emptySet();
        return userRepository.findByEmail(userDetails.getUsername())
                .map(user -> {
                    List<Wishlist> items = wishlistRepository.findByUserId(user.getId());
                    Set<Long> ids = new HashSet<>();
                    for (Wishlist w : items) ids.add(w.getProduct().getId());
                    return ids;
                })
                .orElse(Collections.emptySet());
    }

    @GetMapping
    public String listProducts(Model model,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) BigDecimal minPrice,
                               @RequestParam(required = false) BigDecimal maxPrice,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size,
                               @RequestParam(defaultValue = "createdAt") String sortBy,
                               @RequestParam(defaultValue = "desc") String sortDir) {

        if (page < 0) page = 0;
        if (size <= 0) size = 12;

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Dùng findWithFilters cho mọi trường hợp (null = không lọc)
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;
        Page<Product> productPage = productService.findWithFilters(kw, categoryId, minPrice, maxPrice, pageable);

        List<Category> categories = categoryRepository.findAll();

        // Tìm selectedCategory nếu có categoryId
        Category selectedCategory = (categoryId != null)
                ? categoryRepository.findById(categoryId).orElse(null)
                : null;

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        int totalPages = productPage.getTotalPages();
        int pageStart = Math.max(0, page - 2);
        int pageEnd = Math.min(totalPages - 1, page + 2);
        if (pageStart > pageEnd) {
            pageStart = 0;
            pageEnd = 0;
        }
        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);
        model.addAttribute("size", size);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("wishlistProductIds", getWishlistProductIds(userDetails));

        // Build per-product avg rating and review count maps
        Map<Long, Double> productAvgRatings = new HashMap<>();
        Map<Long, Long> productReviewCounts = new HashMap<>();
        reviewRepository.getAverageRatingsByProduct().forEach(row -> {
            productAvgRatings.put((Long) row[0], (Double) row[1]);
            productReviewCounts.put((Long) row[0], (Long) row[2]);
        });
        model.addAttribute("productAvgRatings", productAvgRatings);
        model.addAttribute("productReviewCounts", productReviewCounts);

        return "user/product-list";
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        List<Product> relatedProducts = productService.findLatestProducts(4);

        boolean inWishlist = false;
        boolean hasReviewed = false;
        if (userDetails != null) {
            inWishlist = userRepository.findByEmail(userDetails.getUsername())
                    .map(user -> wishlistRepository.existsByUserAndProduct(user, product))
                    .orElse(false);
            hasReviewed = userRepository.findByEmail(userDetails.getUsername())
                    .map(user -> reviewService.hasReviewed(user.getId(), id))
                    .orElse(false);
        }

        List<Review> reviews = reviewService.getApprovedReviews(id);
        double avgRating = reviewService.getAverageRating(id);

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        model.addAttribute("inWishlist", inWishlist);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("hasReviewed", hasReviewed);
        model.addAttribute("isLoggedIn", userDetails != null);

        return "user/product-detail";
    }

    @GetMapping("/category/{categoryId}")
    public String productsByCategory(@PathVariable Long categoryId,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model) {
        // Redirect sang /products?categoryId=... để dùng chung filter
        return "redirect:/products?categoryId=" + categoryId + "&page=" + page;
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam String keyword,
                                @AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        return "redirect:/products?keyword=" + keyword + "&page=" + page;
    }
}
