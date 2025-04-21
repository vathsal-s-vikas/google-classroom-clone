package com.classroom.controller;

import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.Optional;
import com.classroom.model.UserType;

@Controller
public class RoleController {
    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private UserRepository userRepository;

    // Show role selection form
    @GetMapping("/select-role")
    public String selectRole(@RequestParam("email") String email, Model model) {
        logger.info("Received request to select role for email: {}", email);
        
        // Verify user exists in the database
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (!optionalUser.isPresent()) {
            logger.error("User with email {} not found in database", email);
            return "redirect:/login?error=user_not_found";
        }
        
        model.addAttribute("email", email);
        return "select-role"; // your select-role.html template
    }

    // Handle form submission
    @PostMapping("/assign-role")
    public String assignRole(@RequestParam("email") String email, @RequestParam("role") String role) {
        logger.info("Assigning role {} to user with email {}", role, email);
        
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            
            try {
                UserType userType = UserType.valueOf(role);
                user.setUserType(userType);
                userRepository.save(user);
                logger.info("Successfully assigned role {} to user {}", userType, user.getId());
                
                return "redirect:/dashboard/" + role.toLowerCase(); // student/teacher/ta
            } catch (IllegalArgumentException e) {
                logger.error("Invalid role value: {}", role, e);
                return "redirect:/select-role?email=" + email + "&error=invalid_role";
            } catch (Exception e) {
                logger.error("Error saving user role", e);
                return "redirect:/select-role?email=" + email + "&error=save_failed";
            }
        }
        
        logger.error("User with email {} not found for role assignment", email);
        return "redirect:/login?error=user_not_found";
    }
}
