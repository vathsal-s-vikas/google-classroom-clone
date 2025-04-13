package com.classroom.clone.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();

        // Place user info in session for easy access
        HttpSession session = request.getSession();
        session.setAttribute("user", oauthUser.getUser());

        // Determine where to redirect the user based on their role
        String targetUrl = determineTargetUrl(oauthUser);

        if (response.isCommitted()) {
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(CustomOAuth2User oauthUser) {
        return switch (oauthUser.getUser().getUserType()) {
            case TEACHER -> "/teacher/dashboard";
            case TA -> "/ta/dashboard";
            default -> "/student/dashboard";
        };
    }
}