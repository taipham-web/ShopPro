package com.sportswear.shop.controller.user;

import com.sportswear.shop.repository.CategoryRepository;
import com.sportswear.shop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller trang chủ
 */
@Controller
public class HomeController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("featuredCategories", categoryRepository.findByFeaturedTrue());
        model.addAttribute("featuredProducts", productRepository.findByFeaturedTrue());
        return "user/index";
    }

    @GetMapping("/about")
    public String about() {
        return "user/about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "user/contact";
    }
}
