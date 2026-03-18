package com.sportswear.shop.controller.user;

import com.sportswear.shop.dto.ChangePasswordDTO;
import com.sportswear.shop.dto.UserProfileDTO;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/profile")
public class ProfileController {

    @Autowired
    private IUserService userService;

    @GetMapping
    public String profilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        
        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setFullName(user.getFullName());
        profileDTO.setEmail(user.getEmail());
        profileDTO.setPhone(user.getPhone());
        profileDTO.setAddress(user.getAddress());
        
        model.addAttribute("user", user);
        model.addAttribute("profileForm", profileDTO);
        model.addAttribute("passwordForm", new ChangePasswordDTO());
        
        return "user/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                               @Valid @ModelAttribute("profileForm") UserProfileDTO profileDTO,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            User user = userService.findByEmail(userDetails.getUsername());
            model.addAttribute("user", user);
            model.addAttribute("passwordForm", new ChangePasswordDTO());
            return "user/profile";
        }

        try {
            userService.updateProfile(userDetails.getUsername(), profileDTO);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/user/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("passwordForm") ChangePasswordDTO passwordDTO,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        if (bindingResult.hasErrors()) {
            UserProfileDTO profileDTO = new UserProfileDTO();
            profileDTO.setFullName(user.getFullName());
            profileDTO.setEmail(user.getEmail());
            profileDTO.setPhone(user.getPhone());
            profileDTO.setAddress(user.getAddress());
            
            model.addAttribute("user", user);
            model.addAttribute("profileForm", profileDTO);
            model.addAttribute("activeTab", "password");
            return "user/profile";
        }

        // Kiểm tra mật khẩu xác nhận
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu xác nhận không khớp");
            return "redirect:/user/profile";
        }

        boolean success = userService.changePassword(userDetails.getUsername(), passwordDTO);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        } else {
            redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu hiện tại không đúng");
        }
        
        return "redirect:/user/profile";
    }
}
