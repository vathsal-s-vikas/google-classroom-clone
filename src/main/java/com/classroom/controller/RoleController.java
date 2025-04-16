package com.classroom.controller;

import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.Optional;
import com.classroom.model.UserType;

@Controller
public class RoleController {

    @Autowired
    private UserRepository userRepository;

    // Show role selection form
    @GetMapping("/select-role")
    public String selectRole(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "select-role"; // your select-role.html template
    }

    // Handle form submission
    @PostMapping("/assign-role")
    public String assignRole(@RequestParam("email") String email, @RequestParam("role") String role) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setUserType(UserType.valueOf(role));
            userRepository.save(user);

            return "redirect:/dashboard/" + role.toLowerCase(); // student/teacher/ta
        }
        return "redirect:/login?error";
    }
}
