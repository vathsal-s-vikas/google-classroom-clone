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

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name"); // Optional: capture name too

        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            // Auto-register the user
            user = new User();
            user.setEmail(email);
            user.setName(name); // if your User model has a name field
            user.setUserType(null); // force role selection
            userRepository.save(user);
        }

        // Redirect to role selection if role not set
        if (user.getUserType() == null) {
            response.sendRedirect("/select-role?email=" + email);
        } else {
            // Redirect based on role
            switch (user.getUserType()) {
                case STUDENT -> response.sendRedirect("/dashboard/student");
                case TEACHER -> response.sendRedirect("/dashboard/teacher");
                case TA -> response.sendRedirect("/dashboard/ta");
                default -> response.sendRedirect("/index");
            }
        }
    }
}
