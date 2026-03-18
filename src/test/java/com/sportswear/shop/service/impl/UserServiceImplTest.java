package com.sportswear.shop.service.impl;

import com.sportswear.shop.dto.ChangePasswordDTO;
import com.sportswear.shop.dto.RegisterFormDTO;
import com.sportswear.shop.dto.UserProfileDTO;
import com.sportswear.shop.entity.Role;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.RoleRepository;
import com.sportswear.shop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerUser_shouldCreateRoleWhenMissing() {
        RegisterFormDTO form = new RegisterFormDTO();
        form.setUsername("newuser");
        form.setEmail("new@sportwear.vn");
        form.setPassword("secret");
        form.setFullName("New User");

        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.registerUser(form);

        assertThat(created.getPassword()).isEqualTo("encoded-secret");
        assertThat(created.isEnabled()).isTrue();
        assertThat(created.getRoles()).extracting(Role::getName).contains("ROLE_USER");
    }

    @Test
    void registerUser_shouldUseExistingRole() {
        RegisterFormDTO form = new RegisterFormDTO();
        form.setUsername("newuser");
        form.setEmail("new@sportwear.vn");
        form.setPassword("secret");

        Role role = new Role("ROLE_USER");

        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.registerUser(form);

        assertThat(created.getRoles()).isEqualTo(Set.of(role));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void updateProfile_shouldChangeEmailWhenAvailable() {
        User user = new User();
        user.setEmail("old@sportwear.vn");

        UserProfileDTO profile = new UserProfileDTO();
        profile.setFullName("Updated");
        profile.setPhone("0900");
        profile.setAddress("HN");
        profile.setEmail("new@sportwear.vn");

        when(userRepository.findByEmail("old@sportwear.vn")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@sportwear.vn")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateProfile("old@sportwear.vn", profile);

        assertThat(updated.getEmail()).isEqualTo("new@sportwear.vn");
        assertThat(updated.getFullName()).isEqualTo("Updated");
    }

    @Test
    void updateProfile_whenNewEmailExists_shouldThrow() {
        User user = new User();
        user.setEmail("old@sportwear.vn");

        UserProfileDTO profile = new UserProfileDTO();
        profile.setEmail("used@sportwear.vn");

        when(userRepository.findByEmail("old@sportwear.vn")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("used@sportwear.vn")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile("old@sportwear.vn", profile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email đã được sử dụng");
    }

    @Test
    void changePassword_whenCurrentWrong_shouldReturnFalse() {
        User user = new User();
        user.setEmail("u@sportwear.vn");
        user.setPassword("encoded-old");

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("wrong");
        dto.setNewPassword("newpass");

        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        boolean changed = userService.changePassword("u@sportwear.vn", dto);

        assertThat(changed).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_whenCurrentCorrect_shouldUpdateAndReturnTrue() {
        User user = new User();
        user.setEmail("u@sportwear.vn");
        user.setPassword("encoded-old");

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("old");
        dto.setNewPassword("newpass");

        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");

        boolean changed = userService.changePassword("u@sportwear.vn", dto);

        assertThat(changed).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(userRepository).save(user);
    }

    @Test
    void existsAndFindMethods_shouldDelegateToRepository() {
        User user = new User();
        user.setUsername("user1");
        user.setEmail("u@sportwear.vn");

        when(userRepository.existsByUsername("user1")).thenReturn(true);
        when(userRepository.existsByEmail("u@sportwear.vn")).thenReturn(true);
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("u@sportwear.vn")).thenReturn(Optional.of(user));

        assertThat(userService.existsByUsername("user1")).isTrue();
        assertThat(userService.existsByEmail("u@sportwear.vn")).isTrue();
        assertThat(userService.findByUsername("user1")).isSameAs(user);
        assertThat(userService.findByEmail("u@sportwear.vn")).isSameAs(user);
    }
}
