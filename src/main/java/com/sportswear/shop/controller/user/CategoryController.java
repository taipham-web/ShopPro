package com.sportswear.shop.controller.user;

import com.sportswear.shop.entity.Category;
import com.sportswear.shop.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public String listCategories(
            @RequestParam(required = false) Long selected,
            Model model) {
        
        // Lấy tất cả danh mục
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        
        // Nếu có danh mục được chọn
        if (selected != null) {
            Category selectedCategory = categoryRepository.findById(selected).orElse(null);
            if (selectedCategory != null) {
                model.addAttribute("selectedCategory", selectedCategory);
                model.addAttribute("selectedProducts", selectedCategory.getProducts());
            }
        }
        
        return "user/category-list";
    }

    @GetMapping("/{id}")
    public String categoryDetail(@PathVariable Long id, Model model) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
        
        model.addAttribute("category", category);
        model.addAttribute("products", category.getProducts());
        
        // Lấy danh mục con nếu có
        List<Category> subCategories = categoryRepository.findByParentId(id);
        model.addAttribute("subCategories", subCategories);
        
        return "user/category-detail";
    }
}
