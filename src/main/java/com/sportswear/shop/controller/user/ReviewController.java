package com.sportswear.shop.controller.user;

import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.IReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products")
public class ReviewController {

    @Autowired
    private IReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/{productId}/review")
    public String submitReview(@PathVariable Long productId,
                               @RequestParam int rating,
                               @RequestParam String comment,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (reviewService.hasReviewed(user.getId(), productId)) {
            redirectAttributes.addFlashAttribute("reviewError", "Bạn đã đánh giá sản phẩm này rồi.");
            return "redirect:/products/" + productId;
        }

        if (comment == null || comment.isBlank()) {
            redirectAttributes.addFlashAttribute("reviewError", "Vui lòng nhập nội dung đánh giá.");
            return "redirect:/products/" + productId;
        }

        reviewService.addReview(productId, user.getId(), rating, comment);
        redirectAttributes.addFlashAttribute("reviewSuccess", "Cảm ơn bạn đã đánh giá sản phẩm!");
        return "redirect:/products/" + productId;
    }
}
