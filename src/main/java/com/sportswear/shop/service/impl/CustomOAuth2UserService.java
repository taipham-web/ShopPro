package com.sportswear.shop.service.impl;

import com.sportswear.shop.entity.Role;
import com.sportswear.shop.entity.User;
import com.sportswear.shop.repository.RoleRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.security.CustomOAuth2UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = new DefaultOAuth2UserService().loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauthUser.getAttributes();

        String email = extractEmail(registrationId, attributes);
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_user_info",
                    "Tai khoan " + registrationId + " khong tra ve email.",
                    null
            ));
        }

        String fullName = extractName(attributes);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createOAuthUser(email, fullName, registrationId, attributes));

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(fullName);
        }
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(defaultUserRole());
        }
        userRepository.save(user);

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        return new CustomOAuth2UserPrincipal(
                attributes,
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                authorities
        );
    }

    private User createOAuthUser(String email, String fullName, String registrationId, Map<String, Object> attributes) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(generateUsername(email, registrationId, attributes));
        newUser.setFullName(fullName);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setEnabled(true);
        newUser.setRoles(defaultUserRole());
        return userRepository.save(newUser);
    }

    private Set<Role> defaultUserRole() {
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role("ROLE_USER");
                    role.setDescription("Nguoi dung thong thuong");
                    return roleRepository.save(role);
                });
        roles.add(userRole);
        return roles;
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("email");
        }

        if ("facebook".equals(registrationId)) {
            String email = (String) attributes.get("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            Object id = attributes.get("id");
            return id != null ? "facebook_" + id + "@facebook.local" : null;
        }

        return (String) attributes.get("email");
    }

    private String extractName(Map<String, Object> attributes) {
        String name = (String) attributes.get("name");
        if (name == null || name.isBlank()) {
            return "Nguoi dung moi";
        }
        return name;
    }

    private String generateUsername(String email, String registrationId, Map<String, Object> attributes) {
        String candidate;
        if (email != null && email.contains("@")) {
            candidate = email.substring(0, email.indexOf('@'));
        } else {
            Object id = attributes.get("id");
            candidate = registrationId + "_" + (id != null ? id : UUID.randomUUID().toString().substring(0, 8));
        }

        String normalized = candidate.replaceAll("[^a-zA-Z0-9._-]", "_");
        String username = normalized;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = normalized + "_" + suffix;
            suffix++;
        }
        return username;
    }
}
