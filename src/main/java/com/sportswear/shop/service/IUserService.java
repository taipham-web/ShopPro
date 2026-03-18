package com.sportswear.shop.service;

import com.sportswear.shop.dto.ChangePasswordDTO;
import com.sportswear.shop.dto.RegisterFormDTO;
import com.sportswear.shop.dto.UserProfileDTO;
import com.sportswear.shop.entity.User;

public interface IUserService {
    
    User registerUser(RegisterFormDTO registerForm);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    User findByUsername(String username);
    
    User findByEmail(String email);
    
    User updateProfile(String email, UserProfileDTO profileDTO);
    
    boolean changePassword(String email, ChangePasswordDTO passwordDTO);
}
