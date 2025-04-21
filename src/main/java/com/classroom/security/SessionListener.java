package com.classroom.security;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

@Component
public class SessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        System.out.println("Session created: " + session.getId());
        
        // Set session timeout to 4 hours (in seconds)
        session.setMaxInactiveInterval(4 * 60 * 60);
        
        System.out.println("Session timeout set to: " + session.getMaxInactiveInterval() + " seconds");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        System.out.println("Session destroyed: " + session.getId());
        System.out.println("Reason: " + (session.getAttribute("SESSION_INVALIDATED_REASON") != null ? 
                           session.getAttribute("SESSION_INVALIDATED_REASON") : "Timeout or manual invalidation"));
        System.out.println("Last access time: " + new java.util.Date(session.getLastAccessedTime()));
    }
} 