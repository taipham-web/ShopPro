package com.sportswear.shop.controller.user;

import com.sportswear.shop.entity.Brand;
import com.sportswear.shop.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/brands")
public class BrandController {

    @Autowired
    private BrandRepository brandRepository;

    @GetMapping
    public String listBrands(Model model) {
        List<Brand> brands = brandRepository.findAllByOrderByNameAsc();
        model.addAttribute("brands", brands);
        return "user/brand-list";
    }

    @GetMapping("/{id}")
    public String brandDetail(@PathVariable Long id, Model model) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu"));
        model.addAttribute("brand", brand);
        return "user/brand-detail";
    }
}
