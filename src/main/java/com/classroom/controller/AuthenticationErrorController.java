package com.classroom.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to provide authentication status information to help debug authentication issues
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationErrorController {

    /**
     * Check if the user is authenticated and return status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthenticationStatus() {
        Map<String, Object> response = new HashMap<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            response.put("authenticated", false);
            response.put("message", "No authentication information found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        if (authentication instanceof AnonymousAuthenticationToken) {
            response.put("authenticated", false);
            response.put("message", "Anonymous authentication detected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        if (!authentication.isAuthenticated()) {
            response.put("authenticated", false);
            response.put("message", "Authentication present but not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            response.put("authenticated", true);
            response.put("principal_type", authentication.getPrincipal().getClass().getName());
            response.put("authorities", authentication.getAuthorities());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("authenticated", false);
            response.put("error", "Error retrieving authentication details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 