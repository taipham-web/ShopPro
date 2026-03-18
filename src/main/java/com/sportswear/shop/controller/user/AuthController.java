package com.sportswear.shop.controller.user;

import com.sportswear.shop.dto.RegisterFormDTO;
import com.sportswear.shop.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller đăng ký, đăng nhập
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private IUserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterFormDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterFormDTO registerForm,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        
        // Kiểm tra mật khẩu xác nhận
        if (!registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "auth/register";
        }

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        // Kiểm tra username đã tồn tại
        if (userService.existsByUsername(registerForm.getUsername())) {
            model.addAttribute("error", "Tên đăng nhập đã được sử dụng");
            return "auth/register";
        }

        // Kiểm tra email đã tồn tại
        if (userService.existsByEmail(registerForm.getEmail())) {
            model.addAttribute("error", "Email đã được sử dụng");
            return "auth/register";
        }

        try {
            userService.registerUser(registerForm);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi đăng ký. Vui lòng thử lại.");
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }
}
