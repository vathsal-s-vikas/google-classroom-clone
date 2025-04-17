package com.classroom.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.classroom.service.UserService;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RegistrationController {

    @Autowired
    private UserRepository myUserRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping(value = "/req/signup", consumes = "application/json")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            // Debug logging
            System.out.println("Received registration request: " + user.getEmail());
            System.out.println("Name: " + user.getName());
            System.out.println("UserType: " + user.getUserType());
            System.out.println("Password present: " + (user.getPassword() != null));
            System.out.println("Password empty: " + (user.getPassword() == null || user.getPassword().trim().isEmpty()));
            
            // Create a map to return all validation errors
            Map<String, String> validationErrors = new HashMap<>();
            
            // Validate input
            if (user.getName() == null || user.getName().trim().isEmpty()) {
                validationErrors.put("name", "Name is required");
            }
            
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                validationErrors.put("email", "Email is required");
            }
            
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                validationErrors.put("password", "Password is required");
            }
            
            if (user.getUserType() == null) {
                validationErrors.put("userType", "User type is required");
            }
            
            // If we have validation errors, return them all
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest().body(validationErrors);
            }
            
            // Check if email already exists
            if (myUserRepository.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
            }
            
            // Set password encoding
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            // Set active status
            user.setActive(true);
            
            // Set timestamps
            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            
            // Save user to database
            User savedUser = myUserRepository.save(user);
            
            // Log successful save
            System.out.println("Successfully created user with ID: " + savedUser.getId());
            
            // Remove password before returning
            savedUser.setPassword(null);
            
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Registration failed: " + e.getMessage());
        }
    }

    @GetMapping("/oauth2/success")
    public String handleOAuth2Success(Authentication authentication) {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        // Check if user exists in DB — if not, create one
        userService.processOAuthPostLogin(email, name);

        // Redirect to homepage or dashboard
        return "redirect:/index";
    }

    @GetMapping("/debug/test-user-creation")
    public ResponseEntity<?> testUserCreation() {
        try {
            // Create a test user
            User testUser = new User();
            testUser.setName("Test User");
            testUser.setEmail("test" + System.currentTimeMillis() + "@example.com"); // Unique email
            testUser.setPassword(passwordEncoder.encode("password123"));
            testUser.setUserType(UserType.STUDENT);
            testUser.setActive(true);
            testUser.setCreatedAt(LocalDateTime.now());
            testUser.setUpdatedAt(LocalDateTime.now());
            
            // Try to save to the database
            User savedUser = myUserRepository.save(testUser);
            
            // Check if saved correctly
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", savedUser.getId());
            response.put("userName", savedUser.getName());
            response.put("userEmail", savedUser.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("errorType", e.getClass().getName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping(value = "/debug/echo-request", consumes = "application/json")
    public ResponseEntity<?> echoRequest(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Received debug request with data: " + requestData);
            return ResponseEntity.ok(requestData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing request: " + e.getMessage());
        }
    }
}
