package com.classroom.controller;

import com.classroom.model.user.User;
import com.classroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public String registerUser(@RequestBody User user) {
        userService.register(user);
        return "User registered successfully!";
    }

    @PostMapping("/login")
    public String loginUser(@RequestBody User user) {
        // This is a placeholder. Later, you can integrate real authentication logic using Spring Security.
        return "User login successful!";
    }
}
