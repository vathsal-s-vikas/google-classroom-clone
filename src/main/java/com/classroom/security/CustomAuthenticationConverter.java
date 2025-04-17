package com.classroom.security;

import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
public class CustomAuthenticationConverter {

    private final UserRepository userRepository;

    public CustomAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Call this method after successful authentication to replace the default authentication
     * with one that contains our CustomUserDetails
     */
    public void convertAuthentication() {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        
        // Skip if already using CustomUserDetails
        if (currentAuth == null || currentAuth.getPrincipal() instanceof CustomUserDetails) {
            return;
        }
        
        try {
            Object principal = currentAuth.getPrincipal();
            String email = null;
            
            // Handle different types of principals
            if (principal instanceof org.springframework.security.core.userdetails.User) {
                email = ((org.springframework.security.core.userdetails.User) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            } else if (principal.getClass().getName().contains("DefaultOidcUser") || 
                      principal.getClass().getName().contains("OAuth2User")) {
                // This is an OAuth2 user - try to get the email
                try {
                    // First try to call getEmail() method if it exists
                    try {
                        java.lang.reflect.Method getEmailMethod = principal.getClass().getMethod("getEmail");
                        email = (String) getEmailMethod.invoke(principal);
                    } catch (NoSuchMethodException e) {
                        // Method doesn't exist, try getAttributes()
                    }
                    
                    // If email is still null, try to get it from attributes
                    if (email == null) {
                        java.lang.reflect.Method getAttributesMethod = principal.getClass().getMethod("getAttributes");
                        Map<String, Object> attributes = (Map<String, Object>) getAttributesMethod.invoke(principal);
                        email = (String) attributes.get("email");
                    }
                    
                    System.out.println("OAuth2 email extracted: " + email);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error extracting email from OAuth2 user: " + e.getMessage());
                    return;
                }
            }
            
            // If we got an email, find the user and create a new authentication
            if (email != null) {
                Optional<User> userOptional = userRepository.findByEmail(email);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    CustomUserDetails customUserDetails = new CustomUserDetails(user);
                    
                    // Create new authentication with our custom details
                    Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        customUserDetails,
                        currentAuth.getCredentials(),
                        customUserDetails.getAuthorities()
                    );
                    
                    // Set the new authentication
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in authentication conversion: " + e.getMessage());
        }
    }
} 