package com.classroom.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);
    
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, 
                                Authentication authentication) throws IOException, ServletException {
        if (authentication != null) {
            logger.info("User '{}' logged out successfully", authentication.getName());
        } else {
            logger.info("Logout occurred for unknown user");
        }
        
        // Redirect to login page with logout parameter
        response.sendRedirect("/login?logout");
    }
} 