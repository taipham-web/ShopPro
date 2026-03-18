package com.sportswear.shop.controller.admin;

import com.sportswear.shop.entity.Brand;
import com.sportswear.shop.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/brands")
public class AdminBrandController {

    @Autowired
    private BrandRepository brandRepository;

    @GetMapping
    public String listBrands(Model model) {
        List<Brand> brands = brandRepository.findAll();
        model.addAttribute("brands", brands);
        return "admin/brands/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("brand", new Brand());
        return "admin/brands/form";
    }

    @PostMapping("/create")
    public String createBrand(@ModelAttribute Brand brand, RedirectAttributes redirectAttributes) {
        brandRepository.save(brand);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm thương hiệu thành công!");
        return "redirect:/admin/brands";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Brand> brand = brandRepository.findById(id);
        if (brand.isEmpty()) {
            return "redirect:/admin/brands";
        }
        
        model.addAttribute("brand", brand.get());
        return "admin/brands/form";
    }

    @PostMapping("/edit/{id}")
    public String updateBrand(
            @PathVariable Long id,
            @ModelAttribute Brand brand,
            RedirectAttributes redirectAttributes) {
        
        Optional<Brand> existingBrand = brandRepository.findById(id);
        if (existingBrand.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thương hiệu!");
            return "redirect:/admin/brands";
        }
        
        Brand updatedBrand = existingBrand.get();
        updatedBrand.setName(brand.getName());
        updatedBrand.setDescription(brand.getDescription());
        updatedBrand.setLogoUrl(brand.getLogoUrl());
        
        brandRepository.save(updatedBrand);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thương hiệu thành công!");
        
        return "redirect:/admin/brands";
    }

    @PostMapping("/delete/{id}")
    public String deleteBrand(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Brand> brand = brandRepository.findById(id);
        if (brand.isPresent()) {
            brandRepository.delete(brand.get());
            redirectAttributes.addFlashAttribute("successMessage", "Xóa thương hiệu thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thương hiệu!");
        }
        return "redirect:/admin/brands";
    }
}
