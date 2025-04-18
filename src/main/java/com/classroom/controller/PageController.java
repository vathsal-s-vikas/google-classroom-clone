package com.classroom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Comment out the entire controller to avoid mapping conflicts with LandingController
//@Controller
public class PageController {
    
    /**
     * Handle login page
     */
    //@GetMapping("/login")
    public String login() {
        return "login";
    }
    
    /**
     * Handle signup page
     */
    //@GetMapping("/signup")
    public String signup() {
        return "signup";
    }
    
    /**
     * Handle /index - redirect to login
     */
    //@GetMapping("/index")
    public String indexPage() {
        return "redirect:/login";
    }
} 