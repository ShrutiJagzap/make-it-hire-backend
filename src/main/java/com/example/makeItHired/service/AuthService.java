package com.example.makeItHired.service;

import com.example.makeItHired.dto.LoginRequest;
import com.example.makeItHired.dto.RegisterRequest;

import com.example.makeItHired.entity.Role;
import com.example.makeItHired.entity.User;
import com.example.makeItHired.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthService {
    private final UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository){

        this.userRepository = userRepository;
    }

    //Register User
    public String register(RegisterRequest req){
        if(userRepository.existsByEmail(req.getEmail())){
            throw new RuntimeException("Email Already exists");
        }
        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
//        user.setRole(req.getRole() == null ? "USER" : req.getRole());
        if("ADMIN".equalsIgnoreCase(req.getRole())) {
            user.setRole(Role.ADMIN);
        } else {
            user.setRole(Role.USER);
        }

        userRepository.save(user);
        return "Registration successful";
    }

    //login
    public User login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid_password");
        }
        return user;
    }
}
