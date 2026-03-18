package com.sportswear.shop.service.impl;

import com.sportswear.shop.entity.Role;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_shouldMapUserAndAuthorities() {
        Role role = new Role("ROLE_USER");

        User user = new User();
        user.setEmail("u@sportwear.vn");
        user.setPassword("encoded");
        user.setEnabled(true);
        user.setRoles(Set.of(role));

        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("u@sportwear.vn");

        assertThat(details.getUsername()).isEqualTo("u@sportwear.vn");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    void loadUserByUsername_whenNotFound_shouldThrow() {
        when(userRepository.findByEmail("missing@sportwear.vn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@sportwear.vn"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Không tìm thấy user với email");
    }
}
