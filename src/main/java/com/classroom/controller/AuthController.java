//package com.classroom.controller;
//
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//
//@Controller
//public class AuthController {
//
//    @GetMapping("/")
//    public String home() {
//        return "home"; // home.html (public page)
//    }
//
//    @GetMapping("/dashboard")
//    public String dashboard() {
//        return "dashboard"; // dashboard.html (secured page)
//    }
//
//    @GetMapping("/login")
//    public String login() {
//        return "login"; // optional if you want a custom login page
//    }
//
//
//    @GetMapping("/dashboard")
//    public String dashboard(Model model, @AuthenticationPrincipal OAuth2User principal) {
//        if (principal != null) {
//            model.addAttribute("name", principal.getAttribute("name"));
//            model.addAttribute("email", principal.getAttribute("email"));
//            model.addAttribute("picture", principal.getAttribute("picture"));
//        }
//        return "dashboard";
//    }
//
//}
