package com.classroom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFilter.class);
    private final CustomAuthenticationConverter authenticationConverter;

    public CustomAuthenticationFilter(CustomAuthenticationConverter authenticationConverter) {
        this.authenticationConverter = authenticationConverter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        boolean isApiRequest = path.startsWith("/api/");
        boolean isTeamEndpoint = path.contains("/api/teams");
        
        // For all API requests, check authentication
        if (isApiRequest) {
            logger.debug("API Request: {} ({})", path, request.getMethod());
            
            if (authentication != null && authentication.isAuthenticated()) {
                logger.info("Processing authenticated API request: {} for user: {}", path, authentication.getName());
                
                // Directly use convert method to get a new authentication object
                Authentication convertedAuth = authenticationConverter.convert(authentication);
                
                // Only set in context if conversion was successful and different
                if (convertedAuth != null && convertedAuth != authentication) {
                    SecurityContextHolder.getContext().setAuthentication(convertedAuth);
                    logger.debug("Set converted authentication in security context");
                }
                
                // Re-get authentication after conversion
                Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                if (currentAuth != null && currentAuth.getPrincipal() instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) currentAuth.getPrincipal();
                    logger.debug("Request authenticated with CustomUserDetails for user: {}", userDetails.getUsername());
                } else if (isTeamEndpoint) {
                    // For team endpoints, this is critical
                    logger.warn("Team endpoint without CustomUserDetails: {} - Principal: {}", 
                        path, 
                        currentAuth != null && currentAuth.getPrincipal() != null ? 
                            currentAuth.getPrincipal().getClass().getName() : "null");
                }
            } else {
                logger.warn("No valid authentication for API request: {}", path);
                
                // For team endpoints specifically
                if (isTeamEndpoint) {
                    logger.error("Team endpoint without authentication: {}", path);
                    // Log request details for debugging
                    logRequestDetails(request);
                }
            }
        } else {
            // For non-API requests, we still want to do the conversion but only log debug info
            if (authentication != null && authentication.isAuthenticated()) {
                logger.debug("Converting authentication for page request: {}", path);
                
                Authentication convertedAuth = authenticationConverter.convert(authentication);
                if (convertedAuth != null && convertedAuth != authentication) {
                    SecurityContextHolder.getContext().setAuthentication(convertedAuth);
                }
            }
        }
        
        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Log additional request details to help with debugging
     */
    private void logRequestDetails(HttpServletRequest request) {
        logger.info("Request method: {}", request.getMethod());
        logger.info("Remote address: {}", request.getRemoteAddr());
        
        // Log headers
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Skip logging authorization header contents for security
            if (!headerName.equalsIgnoreCase("authorization") && 
                !headerName.equalsIgnoreCase("cookie")) {
                logger.info("Header {}: {}", headerName, request.getHeader(headerName));
            } else {
                logger.info("Header {}: [REDACTED]", headerName);
            }
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip for static resources and login-related paths
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.equals("/login") ||
               path.equals("/logout") ||
               path.startsWith("/oauth2/");
    }
} 