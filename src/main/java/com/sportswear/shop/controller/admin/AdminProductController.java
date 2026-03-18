package com.sportswear.shop.controller.admin;

import com.sportswear.shop.entity.Product;
import com.sportswear.shop.repository.BrandRepository;
import com.sportswear.shop.repository.CategoryRepository;
import com.sportswear.shop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Value("${app.upload.dir:src/main/resources/static/images/products}")
    private String uploadDir;

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        Page<Product> products;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (keyword != null && !keyword.isEmpty()) {
            products = productRepository.searchProducts(keyword, pageRequest);
        } else {
            products = productRepository.findAll(pageRequest);
        }
        
        model.addAttribute("products", products.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalItems", products.getTotalElements());
        model.addAttribute("keyword", keyword);
        
        return "admin/products/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        return "admin/products/form";
    }

    @PostMapping("/create")
    public String createProduct(
            @ModelAttribute Product product,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) MultipartFile[] imageFiles,
            @RequestParam(required = false) List<String> sizes,
            RedirectAttributes redirectAttributes) {
        try {
            // Ảnh chính
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = saveImage(imageFile);
                product.setImage("/images/products/" + fileName);
            }
            // Ảnh phụ
            if (imageFiles != null && imageFiles.length > 0) {
                for (MultipartFile file : imageFiles) {
                    if (file != null && !file.isEmpty()) {
                        String fileName = saveImage(file);
                        product.getImages().add("/images/products/" + fileName);
                    }
                }
            }
            // Sizes: dùng danh sách từ form
            if (sizes != null && !sizes.isEmpty()) {
                product.setSizes(sizes);
            } else {
                product.setSizes(new ArrayList<>());
            }
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải ảnh: " + e.getMessage());
            return "redirect:/admin/products/create";
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            return "redirect:/admin/products";
        }
        
        model.addAttribute("product", product.get());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("brands", brandRepository.findAll());
        return "admin/products/form";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @ModelAttribute Product product,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) MultipartFile[] imageFiles,
            @RequestParam(required = false) List<String> sizes,
            RedirectAttributes redirectAttributes) {
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }
        try {
            Product updatedProduct = existingProduct.get();
            updatedProduct.setName(product.getName());
            updatedProduct.setDescription(product.getDescription());
            updatedProduct.setPrice(product.getPrice());
            updatedProduct.setSalePrice(product.getSalePrice());
            updatedProduct.setQuantity(product.getQuantity());
            updatedProduct.setCategory(product.getCategory());
            updatedProduct.setActive(product.isActive());
            updatedProduct.setFeatured(product.isFeatured());
            // Sizes
            if (sizes != null && !sizes.isEmpty()) {
                updatedProduct.setSizes(sizes);
            } else {
                updatedProduct.setSizes(new ArrayList<>());
            }
            // Ảnh chính
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = saveImage(imageFile);
                updatedProduct.setImage("/images/products/" + fileName);
            }
            // Ảnh phụ (thêm mới, không xóa ảnh cũ)
            if (imageFiles != null && imageFiles.length > 0) {
                for (MultipartFile file : imageFiles) {
                    if (file != null && !file.isEmpty()) {
                        String fileName = saveImage(file);
                        updatedProduct.getImages().add("/images/products/" + fileName);
                    }
                }
            }
            productRepository.save(updatedProduct);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải ảnh: " + e.getMessage());
            return "redirect:/admin/products/edit/" + id;
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            productRepository.delete(product.get());
            redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/toggle-active/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product p = product.get();
            p.setActive(!p.isActive());
            productRepository.save(p);
            redirectAttributes.addFlashAttribute("successMessage", 
                p.isActive() ? "Đã kích hoạt sản phẩm!" : "Đã ẩn sản phẩm!");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/toggle-featured/{id}")
    public String toggleFeatured(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product p = product.get();
            p.setFeatured(!p.isFeatured());
            productRepository.save(p);
            redirectAttributes.addFlashAttribute("successMessage",
                p.isFeatured() ? "Đã đánh dấu nổi bật!" : "Đã bỏ nổi bật!");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/edit/{id}/remove-image")
    public String removeImage(
            @PathVariable Long id,
            @RequestParam int imageIndex,
            RedirectAttributes redirectAttributes) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            if (imageIndex >= 0 && imageIndex < product.getImages().size()) {
                product.getImages().remove(imageIndex);
                productRepository.save(product);
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh phụ!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy ảnh!");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
        }
        return "redirect:/admin/products/edit/" + id;
    }

    private String saveImage(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return fileName;
    }
}
