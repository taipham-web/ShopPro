package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.user.ProfileController;
import com.sportswear.shop.dto.ChangePasswordDTO;
import com.sportswear.shop.dto.UserProfileDTO;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.IUserService;
import com.sportswear.shop.service.impl.CustomOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

        @MockitoBean
        private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void updateProfileSuccess_shouldRedirectProfile() throws Exception {
        mockMvc.perform(post("/user/profile/update")
                        .with(csrf())
                        .param("fullName", "Updated User")
                        .param("email", "user@sportwear.vn")
                        .param("phone", "0900000000")
                        .param("address", "New Address"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile"));

        verify(userService).updateProfile(org.mockito.ArgumentMatchers.eq("user@sportwear.vn"),
                org.mockito.ArgumentMatchers.any(UserProfileDTO.class));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void changePasswordSuccess_shouldRedirectProfile() throws Exception {
        when(userService.changePassword(org.mockito.ArgumentMatchers.eq("user@sportwear.vn"), org.mockito.ArgumentMatchers.any(ChangePasswordDTO.class)))
                .thenReturn(true);

        User user = new User();
        user.setEmail("user@sportwear.vn");
        when(userService.findByEmail("user@sportwear.vn")).thenReturn(user);

        mockMvc.perform(post("/user/profile/change-password")
                        .with(csrf())
                        .param("currentPassword", "oldpass")
                        .param("newPassword", "newpass123")
                        .param("confirmPassword", "newpass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile"));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void changePasswordWrongCurrent_shouldRedirectProfileWithError() throws Exception {
        when(userService.changePassword(org.mockito.ArgumentMatchers.eq("user@sportwear.vn"), org.mockito.ArgumentMatchers.any(ChangePasswordDTO.class)))
                .thenReturn(false);

        User user = new User();
        user.setEmail("user@sportwear.vn");
        when(userService.findByEmail("user@sportwear.vn")).thenReturn(user);

        mockMvc.perform(post("/user/profile/change-password")
                        .with(csrf())
                        .param("currentPassword", "wrong")
                        .param("newPassword", "newpass123")
                        .param("confirmPassword", "newpass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile"));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void changePasswordConfirmMismatch_shouldRedirectProfile() throws Exception {
        User user = new User();
        user.setEmail("user@sportwear.vn");
        user.setFullName("Test User");
        when(userService.findByEmail("user@sportwear.vn")).thenReturn(user);

        mockMvc.perform(post("/user/profile/change-password")
                        .with(csrf())
                        .param("currentPassword", "oldpass")
                        .param("newPassword", "newpass123")
                        .param("confirmPassword", "different123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile"));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void updateProfileInvalidEmail_shouldReturnProfileView() throws Exception {
        User user = new User();
        user.setEmail("user@sportwear.vn");
        user.setFullName("Test User");
        when(userService.findByEmail("user@sportwear.vn")).thenReturn(user);

        mockMvc.perform(post("/user/profile/update")
                        .with(csrf())
                        .param("fullName", "Updated User")
                        .param("email", "invalid-email")
                        .param("phone", "0900000000")
                        .param("address", "Address"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"));
    }
}
