package com.classroom.security;

import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
@SuppressWarnings("unchecked") // Suppress unchecked warnings across the class
public class CustomAuthenticationConverter {

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationConverter.class);

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
        
        logger.info("Converting authentication: {}", currentAuth);
        logger.info("Authentication class: {}", currentAuth.getClass().getName());
        logger.info("Principal class: {}", currentAuth.getPrincipal() != null ? currentAuth.getPrincipal().getClass().getName() : "null");
        
        try {
            Object principal = currentAuth.getPrincipal();
            String email = null;
            
            // Handle different types of principals
            if (principal instanceof org.springframework.security.core.userdetails.User) {
                email = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                logger.info("Extracted email from User: {}", email);
            } else if (principal instanceof String) {
                email = (String) principal;
                logger.info("Principal is a String: {}", email);
            } else if (principal.getClass().getName().contains("DefaultOidcUser") || 
                      principal.getClass().getName().contains("OAuth2User")) {
                // This is an OAuth2 user - try to get the email
                try {
                    logger.info("Principal is OAuth2User type, extracting attributes...");
                    // Print all available attributes for debugging
                    try {
                        java.lang.reflect.Method getAttributesMethod = principal.getClass().getMethod("getAttributes");
                        Map<String, Object> attributes = (Map<String, Object>) getAttributesMethod.invoke(principal);
                        logger.info("Available OAuth attributes: {}", attributes.keySet());
                    } catch (Exception e) {
                        logger.error("Failed to get attributes for debug: {}", e.getMessage());
                    }
                    
                    // First try to call getEmail() method if it exists
                    try {
                        java.lang.reflect.Method getEmailMethod = principal.getClass().getMethod("getEmail");
                        email = (String) getEmailMethod.invoke(principal);
                        logger.info("Email from getEmail() method: {}", email);
                    } catch (NoSuchMethodException e) {
                        // Method doesn't exist, try getAttributes()
                        logger.info("No getEmail() method, trying attributes map");
                    }
                    
                    // If email is still null, try to get it from attributes
                    if (email == null) {
                        java.lang.reflect.Method getAttributesMethod = principal.getClass().getMethod("getAttributes");
                        Map<String, Object> attributes = (Map<String, Object>) getAttributesMethod.invoke(principal);
                        
                        // Try common email attribute names
                        if (attributes.containsKey("email")) {
                            email = (String) attributes.get("email");
                            logger.info("Email from 'email' attribute: {}", email);
                        } else if (attributes.containsKey("mail")) {
                            email = (String) attributes.get("mail");
                            logger.info("Email from 'mail' attribute: {}", email);
                        } else if (attributes.containsKey("preferred_username")) {
                            email = (String) attributes.get("preferred_username");
                            logger.info("Email from 'preferred_username' attribute: {}", email);
                        } else {
                            // Try to find an attribute that looks like an email
                            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                                if (entry.getValue() instanceof String && 
                                    ((String)entry.getValue()).contains("@")) {
                                    email = (String)entry.getValue();
                                    logger.info("Email from attribute '{}': {}", entry.getKey(), email);
                                    break;
                                }
                            }
                        }
                    }
                    
                    logger.info("Final OAuth2 email extracted: {}", email);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Error extracting email from OAuth2 user: {}", e.getMessage());
                    return;
                }
            } else {
                logger.warn("Unhandled principal type: {}", principal != null ? principal.getClass().getName() : "null");
            }
            
            // If we got an email, find the user and create a new authentication
            if (email != null) {
                Optional<User> userOptional = userRepository.findByEmail(email);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    logger.info("Found user in repository: {}", user.getEmail());
                    CustomUserDetails customUserDetails = new CustomUserDetails(user);
                    
                    // Create new authentication with our custom details
                    Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        customUserDetails,
                        currentAuth.getCredentials(),
                        customUserDetails.getAuthorities()
                    );
                    
                    // Set the new authentication
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                    logger.info("Converted authentication successfully for user: {}", user.getEmail());
                } else {
                    logger.warn("No user found with email: {}", email);
                }
            } else {
                logger.warn("Failed to extract email from principal");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in authentication conversion: {}", e.getMessage());
        }
    }

    public Authentication convert(Authentication authentication) {
        if (authentication == null) {
            logger.info("Authentication is null");
            return null;
        }

        logger.info("Converting authentication: {}", authentication);
        logger.info("Authentication class: {}", authentication.getClass().getName());
        
        Object principal = authentication.getPrincipal();
        
        if (principal == null) {
            logger.warn("Principal is null in authentication");
            return authentication;
        }
        
        logger.info("Principal class: {}", principal.getClass().getName());
        
        // Already converted
        if (principal instanceof CustomUserDetails) {
            logger.info("Principal is already a CustomUserDetails instance: {}", 
                ((CustomUserDetails)principal).getUsername());
            return authentication;
        }

        String email = null;
        
        // Try to extract email from different principal types
        try {
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
                logger.info("Extracted email from UserDetails: {}", email);
            } else if (principal instanceof DefaultOidcUser) {
                DefaultOidcUser oidcUser = (DefaultOidcUser) principal;
                OidcIdToken idToken = oidcUser.getIdToken();
                if (idToken != null && idToken.getEmail() != null) {
                    email = idToken.getEmail();
                    logger.info("Extracted email from OIDC token: {}", email);
                } else {
                    // Try to get from attributes
                    Map<String, Object> attributes = oidcUser.getAttributes();
                    logger.debug("OIDC attributes: {}", attributes.keySet());
                    
                    if (attributes.containsKey("email")) {
                        email = (String) attributes.get("email");
                        logger.info("Extracted email from OIDC 'email' attribute: {}", email);
                    } else if (attributes.containsKey("mail")) {
                        email = (String) attributes.get("mail");
                        logger.info("Extracted email from OIDC 'mail' attribute: {}", email);
                    } else if (attributes.containsKey("preferred_username")) {
                        email = (String) attributes.get("preferred_username");
                        logger.info("Extracted email from OIDC 'preferred_username' attribute: {}", email);
                    }
                }
            } else if (principal instanceof DefaultOAuth2User) {
                DefaultOAuth2User oauth2User = (DefaultOAuth2User) principal;
                Map<String, Object> attributes = oauth2User.getAttributes();
                logger.debug("OAuth2 attributes: {}", attributes.keySet());
                
                if (attributes.containsKey("email")) {
                    email = (String) attributes.get("email");
                    logger.info("Extracted email from OAuth2 'email' attribute: {}", email);
                } else if (attributes.containsKey("mail")) {
                    email = (String) attributes.get("mail");
                    logger.info("Extracted email from OAuth2 'mail' attribute: {}", email);
                } else if (attributes.containsKey("preferred_username")) {
                    email = (String) attributes.get("preferred_username");
                    logger.info("Extracted email from OAuth2 'preferred_username' attribute: {}", email);
                } else {
                    // Try to find an attribute that looks like an email
                    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                        if (entry.getValue() instanceof String && 
                            ((String)entry.getValue()).contains("@")) {
                            email = (String)entry.getValue();
                            logger.info("Extracted email from OAuth2 attribute '{}': {}", entry.getKey(), email);
                            break;
                        }
                    }
                }
            } else if (principal instanceof String) {
                email = (String) principal;
                logger.info("Principal is a String: {}", email);
            } else {
                // Try reflection as a last resort
                try {
                    java.lang.reflect.Method getAttributesMethod = principal.getClass().getMethod("getAttributes");
                    Map<String, Object> attributes = (Map<String, Object>) getAttributesMethod.invoke(principal);
                    
                    if (attributes.containsKey("email")) {
                        email = (String) attributes.get("email");
                        logger.info("Extracted email using reflection from 'email' attribute: {}", email);
                    } else if (attributes.containsKey("mail")) {
                        email = (String) attributes.get("mail");
                        logger.info("Extracted email using reflection from 'mail' attribute: {}", email);
                    } else if (attributes.containsKey("preferred_username")) {
                        email = (String) attributes.get("preferred_username");
                        logger.info("Extracted email using reflection from 'preferred_username' attribute: {}", email);
                    }
                } catch (Exception e) {
                    logger.warn("Could not extract attributes via reflection: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting email from principal: {}", e.getMessage(), e);
        }

        if (email == null) {
            logger.warn("Email could not be extracted from principal of type {}", 
                principal.getClass().getName());
            return authentication;
        }

        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                logger.info("Found user in repository: {} (ID: {})", user.getEmail(), user.getId());
                
                CustomUserDetails userDetails = new CustomUserDetails(user);
                
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    authentication.getCredentials(),
                    userDetails.getAuthorities()
                );
                
                logger.info("Created new authentication with CustomUserDetails for user: {}", user.getEmail());
                return newAuth;
            } else {
                logger.warn("User not found in repository for email: {}", email);
            }
        } catch (Exception e) {
            logger.error("Error during user lookup or authentication conversion", e);
        }
        
        logger.warn("Returning original authentication as conversion failed for: {}", 
            authentication.getName());
        return authentication;
    }
} 