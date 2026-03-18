package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.user.AuthController;
import com.sportswear.shop.dto.RegisterFormDTO;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.IUserService;
import com.sportswear.shop.service.impl.CustomOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

        @MockitoBean
        private CartService cartService;

    @Test
    void registerSuccess_shouldRedirectToLogin() throws Exception {
        when(userService.existsByUsername("newuser")).thenReturn(false);
        when(userService.existsByEmail("new@example.com")).thenReturn(false);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("fullName", "New User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("success"));

        ArgumentCaptor<RegisterFormDTO> captor = ArgumentCaptor.forClass(RegisterFormDTO.class);
        verify(userService).registerUser(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void registerDuplicateEmail_shouldStayOnRegisterPage() throws Exception {
        when(userService.existsByUsername("newuser")).thenReturn(false);
        when(userService.existsByEmail("new@example.com")).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

        @Test
        void registerPasswordMismatch_shouldStayOnRegisterPage() throws Exception {
                mockMvc.perform(post("/auth/register")
                                                .with(csrf())
                                                .param("username", "newuser")
                                                .param("email", "new@example.com")
                                                .param("password", "secret123")
                                                .param("confirmPassword", "different123"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("auth/register"));
        }

    @Test
    void formLoginUser_shouldRedirectToHome() throws Exception {
        UserDetails user = User.withUsername("user@sportwear.vn")
                .password(passwordEncoder.encode("user123"))
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername(eq("user@sportwear.vn"))).thenReturn(user);

        mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/auth/login")
                        .userParameter("email")
                        .user("user@sportwear.vn")
                        .password("user123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void formLoginAdmin_shouldRedirectToAdmin() throws Exception {
        UserDetails admin = User.withUsername("admin@sportwear.vn")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername(eq("admin@sportwear.vn"))).thenReturn(admin);

        mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/auth/login")
                        .userParameter("email")
                        .user("admin@sportwear.vn")
                        .password("admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void formLoginWrongPassword_shouldRedirectToError() throws Exception {
        UserDetails user = User.withUsername("user@sportwear.vn")
                .password(passwordEncoder.encode("correct123"))
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername(eq("user@sportwear.vn"))).thenReturn(user);

        mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/auth/login")
                        .userParameter("email")
                        .user("user@sportwear.vn")
                        .password("wrong123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error=true"));
    }
}
