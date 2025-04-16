package com.classroom.security;

import com.classroom.model.User;
import com.classroom.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomLoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String email = authentication.getName(); // email = username
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            switch (user.getUserType()) {
                case STUDENT -> response.sendRedirect("/dashboard/student");
                case TEACHER -> response.sendRedirect("/dashboard/teacher");
                case TA -> response.sendRedirect("/dashboard/ta");
                default -> response.sendRedirect("/index"); // fallback
            }
        } else {
            response.sendRedirect("/login?error"); // user not found
        }
    }
}
