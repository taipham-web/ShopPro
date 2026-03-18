package com.sportswear.shop.service.impl;

import com.sportswear.shop.dto.ChangePasswordDTO;
import com.sportswear.shop.dto.RegisterFormDTO;
import com.sportswear.shop.dto.UserProfileDTO;
import com.sportswear.shop.entity.Role;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.RoleRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User registerUser(RegisterFormDTO registerForm) {
        User user = new User();
        user.setUsername(registerForm.getUsername());
        user.setEmail(registerForm.getEmail());
        user.setPassword(passwordEncoder.encode(registerForm.getPassword()));
        user.setFullName(registerForm.getFullName());
        user.setPhone(registerForm.getPhone());
        user.setAddress(registerForm.getAddress());
        user.setEnabled(true);

        // Gán role USER mặc định
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = new Role("ROLE_USER");
                    newRole.setDescription("Người dùng thông thường");
                    return roleRepository.save(newRole);
                });
        roles.add(userRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional
    public User updateProfile(String email, UserProfileDTO profileDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        user.setFullName(profileDTO.getFullName());
        user.setPhone(profileDTO.getPhone());
        user.setAddress(profileDTO.getAddress());
        
        // Nếu email thay đổi, kiểm tra email mới có tồn tại chưa
        if (!email.equals(profileDTO.getEmail())) {
            if (userRepository.existsByEmail(profileDTO.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            user.setEmail(profileDTO.getEmail());
        }
        
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public boolean changePassword(String email, ChangePasswordDTO passwordDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(passwordDTO.getCurrentPassword(), user.getPassword())) {
            return false;
        }
        
        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        userRepository.save(user);
        return true;
    }
}
