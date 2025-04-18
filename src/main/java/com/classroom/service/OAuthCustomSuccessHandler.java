package com.classroom.service;

import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuthCustomSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public OAuthCustomSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Log the authentication success
        System.out.println("OAuth authentication successful");
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name"); // Optional: capture name too

        System.out.println("OAuth user email: " + email);
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            System.out.println("Existing user found with ID: " + user.getId() + ", UserType: " + user.getUserType());
        } else {
            // Auto-register the user
            user = new User();
            user.setEmail(email);
            user.setName(name); // if your User model has a name field
            user.setUserType(null); // force role selection
            user.setActive(true);
            user.setPassword("OAUTH_USER"); // Dummy password for OAuth users
            user = userRepository.save(user);
            System.out.println("New user created with ID: " + user.getId());
        }

        // Redirect to role selection if role not set
        if (user.getUserType() == null) {
            System.out.println("User has no role - redirecting to select-role page");
            
            // Make sure the select-role endpoint is properly configured to be accessible
            String selectRoleUrl = "/select-role?email=" + email;
            System.out.println("Redirecting to: " + selectRoleUrl);
            
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
            
            System.out.println("User has role: " + user.getUserType() + " - redirecting to: " + redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }
}
