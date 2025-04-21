package com.classroom.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Get error details
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;
        String errorMessage = message != null ? message.toString() : "An error occurred";
        String path = requestUri != null ? requestUri.toString() : request.getRequestURI();
        
        // Log the error for debugging
        System.err.println("ERROR OCCURRED: " + new Date());
        System.err.println("Status: " + statusCode);
        System.err.println("Path: " + path);
        System.err.println("Message: " + errorMessage);
        if (exception != null) {
            System.err.println("Exception: " + exception);
            if (exception instanceof Exception) {
                ((Exception) exception).printStackTrace();
            }
        }
        
        // Check for authentication-related errors
        if (statusCode == 401 || statusCode == 403) {
            System.err.println("Authentication/Authorization Error on path: " + path);
            // For API endpoints, check authentication context
            if (path.startsWith("/api/")) {
                System.err.println("API Authentication Error - Check security context");
            }
        }
        
        // Add attributes to model
        model.addAttribute("timestamp", new Date());
        model.addAttribute("status", statusCode);
        model.addAttribute("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        model.addAttribute("message", errorMessage);
        model.addAttribute("path", path);
        
        // Return the error view
        return "error";
    }
    
    @RequestMapping("/error/api")
    @ResponseBody
    public Map<String, Object> handleApiError(HttpServletRequest request) {
        // Get error details
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;
        String errorMessage = message != null ? message.toString() : "An error occurred";
        String path = requestUri != null ? requestUri.toString() : request.getRequestURI();
        
        // Log the error for debugging
        System.err.println("API ERROR OCCURRED: " + new Date());
        System.err.println("Status: " + statusCode);
        System.err.println("Path: " + path);
        System.err.println("Message: " + errorMessage);
        if (exception != null) {
            System.err.println("Exception: " + exception);
            if (exception instanceof Exception) {
                ((Exception) exception).printStackTrace();
            }
        }
        
        // Create JSON response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", new Date());
        errorResponse.put("status", statusCode);
        errorResponse.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        errorResponse.put("message", errorMessage);
        errorResponse.put("path", path);
        
        return errorResponse;
    }
} 