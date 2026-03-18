package com.sportswear.shop.config;

import com.sportswear.shop.entity.Role;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.RoleRepository;
import com.sportswear.shop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Tạo role ADMIN nếu chưa có
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role("ROLE_ADMIN");
                    role.setDescription("Quản trị viên");
                    return roleRepository.save(role);
                });

        // Tạo role USER nếu chưa có
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role("ROLE_USER");
                    role.setDescription("Người dùng thông thường");
                    return roleRepository.save(role);
                });

        // Tạo tài khoản admin nếu chưa có
        if (!userRepository.existsByEmail("admin@sportwear.vn")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@sportwear.vn");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setPhone("0123456789");
            admin.setEnabled(true);

            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);

            userRepository.save(admin);
            
            System.out.println("==============================================");
            System.out.println("Tài khoản Admin đã được tạo:");
            System.out.println("Email: admin@sportwear.vn");
            System.out.println("Mật khẩu: admin123");
            System.out.println("==============================================");
        }
    }
}
