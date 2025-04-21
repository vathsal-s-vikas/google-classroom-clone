package com.classroom.service;

import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.time.LocalDateTime;

@Component
public class OAuthCustomSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuthCustomSuccessHandler.class);
    private final UserRepository userRepository;

    public OAuthCustomSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Log the authentication success
        logger.info("OAuth authentication successful");
        
        if (!(authentication.getPrincipal() instanceof OAuth2User)) {
            logger.error("Principal is not an OAuth2User: {}", authentication.getPrincipal().getClass().getName());
            response.sendRedirect("/login?error=invalid_oauth");
            return;
        }
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        // Extract email from the OAuth2User attributes
        String email = oAuth2User.getAttribute("email");
        if (email == null || email.trim().isEmpty()) {
            logger.error("Email not found in OAuth2User attributes");
            response.sendRedirect("/login?error=no_email");
            return;
        }
        
        String name = oAuth2User.getAttribute("name");
        if (name == null || name.trim().isEmpty()) {
            // Fallback to email if name is not provided
            logger.warn("Name not found in OAuth2User attributes, using email prefix");
            name = email.split("@")[0];
        }

        logger.info("OAuth user email: {}", email);
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        try {
            if (userOptional.isPresent()) {
                user = userOptional.get();
                logger.info("Existing user found with ID: {}, UserType: {}", user.getId(), user.getUserType());
                
                // Update last login time
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            } else {
                // Auto-register the user
                logger.info("Creating new user with email: {}", email);
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setFirstName(name.split(" ").length > 0 ? name.split(" ")[0] : name);
                user.setLastName(name.split(" ").length > 1 ? name.split(" ")[1] : "");
                user.setUserType(null); // force role selection
                user.setActive(true);
                user.setGoogleLinked(true);
                user.setOauthId(oAuth2User.getName());
                user.setPassword("OAUTH_USER"); // Dummy password for OAuth users
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                
                user = userRepository.save(user);
                logger.info("New user created with ID: {}", user.getId());
            }
        } catch (Exception e) {
            logger.error("Error creating/updating user during OAuth authentication", e);
            response.sendRedirect("/login?error=user_creation_failed");
            return;
        }

        // Redirect to role selection if role not set
        if (user.getUserType() == null) {
            logger.info("User has no role - redirecting to select-role page");
            
            // Make sure the select-role endpoint is properly configured to be accessible
            String selectRoleUrl = "/select-role?email=" + email;
            logger.info("Redirecting to: {}", selectRoleUrl);
            
            response.sendRedirect(selectRoleUrl);
        } else {
            // Redirect based on role
            String redirectUrl;
            switch (user.getUserType()) {
                case STUDENT -> redirectUrl = "/dashboard/student";
                case TEACHER -> redirectUrl = "/dashboard/teacher";
                case TA -> redirectUrl = "/dashboard/ta";
                default -> redirectUrl = "/login"; // Changed from /index to /login to avoid potential loops
            }
            
            logger.info("User has role: {} - redirecting to: {}", user.getUserType(), redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }
}
