package com.sportswear.shop.controller.admin;

import com.sportswear.shop.entity.Category;
import com.sportswear.shop.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return "admin/categories/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("parentCategories", categoryRepository.findByParentIsNull());
        return "admin/categories/form";
    }

    @PostMapping("/create")
    public String createCategory(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            return "redirect:/admin/categories";
        }
        
        model.addAttribute("category", category.get());
        model.addAttribute("parentCategories", categoryRepository.findByParentIsNull());
        return "admin/categories/form";
    }

    @PostMapping("/edit/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @ModelAttribute Category category,
            RedirectAttributes redirectAttributes) {
        
        Optional<Category> existingCategory = categoryRepository.findById(id);
        if (existingCategory.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
            return "redirect:/admin/categories";
        }
        
        Category updatedCategory = existingCategory.get();
        updatedCategory.setName(category.getName());
        updatedCategory.setDescription(category.getDescription());
        updatedCategory.setImage(category.getImage());
        updatedCategory.setParent(category.getParent());
        updatedCategory.setActive(category.isActive());
        updatedCategory.setFeatured(category.isFeatured());
        
        categoryRepository.save(updatedCategory);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        
        return "redirect:/admin/categories";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            // Kiểm tra có danh mục con không
            List<Category> children = categoryRepository.findByParentId(id);
            if (!children.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Không thể xóa danh mục này vì có " + children.size() + " danh mục con!");
                return "redirect:/admin/categories";
            }
            
            // Kiểm tra có sản phẩm không
            if (!category.get().getProducts().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Không thể xóa danh mục này vì có sản phẩm thuộc danh mục!");
                return "redirect:/admin/categories";
            }
            
            categoryRepository.delete(category.get());
            redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/toggle-active/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            Category c = category.get();
            c.setActive(!c.isActive());
            categoryRepository.save(c);
            redirectAttributes.addFlashAttribute("successMessage", 
                c.isActive() ? "Đã kích hoạt danh mục!" : "Đã ẩn danh mục!");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/toggle-featured/{id}")
    public String toggleFeatured(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            Category c = category.get();
            c.setFeatured(!c.isFeatured());
            categoryRepository.save(c);
            redirectAttributes.addFlashAttribute("successMessage",
                c.isFeatured() ? "Đã đánh dấu nổi bật!" : "Đã bỏ nổi bật!");
        }
        return "redirect:/admin/categories";
    }
}
