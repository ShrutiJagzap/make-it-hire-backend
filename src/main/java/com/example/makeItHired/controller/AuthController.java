package com.example.makeItHired.controller;

import com.example.makeItHired.dto.LoginRequest;
import com.example.makeItHired.dto.RegisterRequest;
import com.example.makeItHired.dto.UpdateProfileRequest;
import com.example.makeItHired.entity.Role;
import com.example.makeItHired.entity.User;
import com.example.makeItHired.repository.UserRepository;
import com.example.makeItHired.service.AuthService;
import com.example.makeItHired.service.NotificationService;


import com.example.makeItHired.service.FirebaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.springframework.data.repository.query.parser.Part;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private FirebaseStorageService firebaseStorageService;

    private final AuthService authService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder, NotificationService notificationService){
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req){
        try{
            if (userRepository.existsByEmail(req.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("message","Email already exist"));
            }

            User user = new User();
            user.setFullName(req.getFullName());
            user.setEmail(req.getEmail());
            user.setPassword(passwordEncoder.encode(req.getPassword()));

            if ("ADMIN".equalsIgnoreCase(req.getRole())) {
                user.setRole(Role.ADMIN);
            }else {
                user.setRole(Role.USER);
            }

            userRepository.save(user);
            try {
                notificationService.createNotification(
                    user.getId(),
                    null,
                    "Welcome to Make It Hire!",
                    "Start by uploading your resume to find your dream job.",
                    "SYSTEM",
                    "/user-dashboard"
                );
            } catch (Exception notifEx) {
                System.err.println("Failed to trigger registration notification: " + notifEx.getMessage());
            }
            return ResponseEntity.badRequest().body(Map.of("message","Registration successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(req.getEmail());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }
            User user = userOpt.get();
            if(!passwordEncoder.matches(req.getPassword(),user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message","Invalid password"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("role", user.getRole().name());
            response.put("idPhotoUrl", user.getIdPhotoUrl() != null ? user.getIdPhotoUrl() : "");

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message","Login failed:" + ex.getMessage()));
        }
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            return ResponseEntity.badRequest().body(java.util.Map.of("message","User_not_Found"));
        }
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User_Not_Found"));
        }

        User user = optionalUser.get();

        // Update common fields
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setTitle(request.getTitle());

        userRepository.save(user);

        return ResponseEntity.ok(user);
    }

    @GetMapping("/profile/image/{fileName}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String fileName) throws IOException {
        Path filePath = Paths.get("uploads/").resolve(fileName);
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"").body(resource);
    }

    @PostMapping("/profile/upload/{id}")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long id, @RequestParam("file")MultipartFile file) {
        try {
            Optional<User> optionalUser = userRepository.findById(id);

            if (optionalUser.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User Not Found"));
            }

            User user = optionalUser.get();

            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName =System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            user.setPhotoUrl(fileName);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("photoUrl", fileName));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "upload Failed"));
        }
    }

    @PostMapping("/register-with-photo")
    public ResponseEntity<?> registerWithPhoto(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String role,
            @RequestParam(value = "idPhoto", required = false) MultipartFile idPhoto) {
        try {
            System.out.println("Registration attempt for email: " + email);

            // Check if user exists
            if (userRepository.existsByEmail(email)) {
                System.out.println("Email already exists: " + email);
                return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
            }

            // Create user
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole("ADMIN".equalsIgnoreCase(role) ? Role.ADMIN : Role.USER);

            // Save ID photo if provided
            if(idPhoto != null && !idPhoto.isEmpty()) {
                try {
                    String uploadDir = "uploads/id_photos/";
                    File dir = new File(uploadDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                        System.out.println("Created directory: " + uploadDir);
                    }

                    String fileName = System.currentTimeMillis() + "_" + idPhoto.getOriginalFilename();
                    Path filePath = Paths.get(uploadDir + fileName);
                    Files.copy(idPhoto.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    user.setIdPhotoUrl(fileName);
                    System.out.println("ID photo saved: " + fileName);
                } catch (Exception e) {
                    System.err.println("Error saving ID photo: " + e.getMessage());
                    // Continue without photo if error
                }
            }

            userRepository.save(user);
            System.out.println("User registered successfully: " + email);
            try {
                notificationService.createNotification(
                    user.getId(),
                    null,
                    "Welcome to Make It Hire!",
                    "Start by uploading your resume to find your dream job.",
                    "SYSTEM",
                    "/user-dashboard"
                );
            } catch (Exception notifEx) {
                System.err.println("Failed to trigger registration notification: " + notifEx.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful");
            response.put("userId", user.getId());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("role", user.getRole().name());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-id-photo")
    public ResponseEntity<?> uploadIdPhoto(
            @RequestParam("userId") Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }
            User user = userOpt.get();

            String uploadDir = "uploads/id_photos/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = System.currentTimeMillis() + "_ " + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            user.setIdPhotoUrl(fileName);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message","Id photo uploaded successfully", "photoUrl", fileName, "userId", userId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Upload failed:" + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "fullName", user.getFullName(),
                    "email", user.getEmail(),
                    "idPhotoUrl", user.getIdPhotoUrl()
            ));
        }
        return ResponseEntity.notFound().build();
    }

}
