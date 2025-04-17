package com.classroom.controller;

import com.classroom.dto.CourseDTO;
import com.classroom.model.Course;
import com.classroom.model.CourseMembership;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomAuthenticationConverter;
import com.classroom.security.CustomUserDetails;
import com.classroom.service.CourseMembershipService;
import com.classroom.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseMembershipService courseMembershipService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomAuthenticationConverter authConverter;

    private User getUserFromAuthentication(Authentication auth) {
        // First try to convert the authentication if needed
        authConverter.convertAuthentication();
        
        // Get the updated authentication
        auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        System.out.println("Principal class: " + principal.getClass().getName());
        
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        } else if (principal instanceof UserDetails) {
            // If it's a regular UserDetails, get the username and fetch the user from repository
            final String username = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } else if (principal instanceof String) {
            // If it's just a username string
            final String username = (String) principal;
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } else if (principal.getClass().getName().contains("DefaultOidcUser") || 
                   principal.getClass().getName().contains("OAuth2User")) {
            // Handle OAuth2 authentication (like Google login)
            try {
                // Use reflection to access the email claim from OAuth
                java.lang.reflect.Method getEmailMethod = principal.getClass().getMethod("getEmail");
                final String email = (String) getEmailMethod.invoke(principal);
                
                if (email != null) {
                    System.out.println("OAuth user email: " + email);
                    return userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("OAuth user not found: " + email));
                }
                
                // Try to get attributes and find email there
                java.lang.reflect.Method getAttributesMethod = principal.getClass().getMethod("getAttributes");
                Map<String, Object> attributes = (Map<String, Object>) getAttributesMethod.invoke(principal);
                final String emailFromAttr = (String) attributes.get("email");
                
                System.out.println("OAuth user email from attributes: " + emailFromAttr);
                
                return userRepository.findByEmail(emailFromAttr)
                        .orElseThrow(() -> new RuntimeException("OAuth user not found: " + emailFromAttr));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error extracting email from OAuth user: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Unsupported principal type: " + principal.getClass().getName());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getTeacherInfo() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final User currentUser = getUserFromAuthentication(auth);
        
        Map<String, String> response = new HashMap<>();
        response.put("name", currentUser.getName());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-courses")
    public ResponseEntity<List<CourseDTO>> getTeacherCourses() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final User currentUser = getUserFromAuthentication(auth);
        
        List<Course> courses = courseService.getCoursesByTeacher(currentUser);
        List<CourseDTO> courseDTOs = courses.stream()
                .map(CourseDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(courseDTOs);
    }

    @PostMapping("/create-course")
    @Transactional
    public ResponseEntity<CourseDTO> createCourse(@RequestBody Map<String, String> courseData) {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final User currentUser = getUserFromAuthentication(auth);
            
            final String name = courseData.get("name");
            final String description = courseData.get("description");
            
            System.out.println("Creating course: " + name + ", " + description);
            System.out.println("Current user: " + currentUser.getName() + ", " + currentUser.getEmail());
            
            // Create a new course
            Course course = new Course();
            course.setName(name);
            course.setDescription(description);
            course.setTeacher(currentUser);
            
            // Generate course code and invite code
            String courseCode = generateCourseCode();
            String inviteCode = generateInviteCode();
            
            course.setCourseCode(courseCode);
            course.setInviteCode(inviteCode);
            course.setArchived(false);
            
            // Save the course
            Course savedCourse = courseService.createCourse(course);
            
            // Create membership for the teacher
            CourseMembership membership = new CourseMembership();
            membership.setCourse(savedCourse);
            membership.setUser(currentUser);
            membership.setRole(UserType.TEACHER);
            
            // Save the membership
            courseMembershipService.addMembership(membership);
            
            // Convert to DTO before returning
            CourseDTO courseDTO = CourseDTO.fromEntity(savedCourse);
            
            return ResponseEntity.ok(courseDTO);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    @GetMapping("/auth-test")
    public ResponseEntity<Map<String, Object>> testAuth() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final Object principal = auth.getPrincipal();
        
        Map<String, Object> response = new HashMap<>();
        response.put("principalType", principal.getClass().getName());
        response.put("authorities", auth.getAuthorities().toString());
        response.put("authenticated", auth.isAuthenticated());
        
        try {
            User user = getUserFromAuthentication(auth);
            response.put("userFound", true);
            response.put("userName", user.getName());
            response.put("userEmail", user.getEmail());
        } catch (Exception e) {
            response.put("userFound", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/oauth-test")
    public ResponseEntity<Map<String, Object>> testOAuth() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final Object principal = auth.getPrincipal();
        
        Map<String, Object> response = new HashMap<>();
        response.put("principalType", principal.getClass().getName());
        response.put("authorities", auth.getAuthorities().toString());
        response.put("authenticated", auth.isAuthenticated());
        
        try {
            // Check if this is an OAuth2 principal
            if (principal.getClass().getName().contains("DefaultOidcUser") || 
                principal.getClass().getName().contains("OAuth2User")) {
                
                // Use reflection to inspect all methods
                java.lang.reflect.Method[] methods = principal.getClass().getMethods();
                Map<String, Object> methodResults = new HashMap<>();
                
                for (java.lang.reflect.Method method : methods) {
                    if (method.getParameterCount() == 0 && 
                        !method.getName().equals("getClass") &&
                        !method.getName().startsWith("wait") &&
                        !method.getName().startsWith("notify") &&
                        !method.getName().startsWith("hashCode") &&
                        !method.getName().startsWith("equals")) {
                        
                        try {
                            final Object result = method.invoke(principal);
                            if (result != null) {
                                methodResults.put(method.getName(), result.toString());
                            }
                        } catch (Exception e) {
                            // Ignore this method
                        }
                    }
                }
                
                response.put("oauthMethods", methodResults);
                
                // Try to get attributes if possible
                try {
                    java.lang.reflect.Method getAttributesMethod = principal.getClass().getMethod("getAttributes");
                    Map<String, Object> attributes = (Map<String, Object>) getAttributesMethod.invoke(principal);
                    response.put("attributes", attributes);
                } catch (Exception e) {
                    response.put("attributesError", e.getMessage());
                }
                
                // Try to get user directly
                try {
                    User user = getUserFromAuthentication(auth);
                    response.put("userFound", true);
                    response.put("userName", user.getName());
                    response.put("userEmail", user.getEmail());
                } catch (Exception e) {
                    response.put("userFound", false);
                    response.put("error", e.getMessage());
                }
            } else {
                response.put("message", "Not an OAuth2 principal");
            }
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    private String generateCourseCode() {
        // Generate a random alphanumeric code
        return "CS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    private String generateInviteCode() {
        // Generate a random invite code
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @DeleteMapping("/delete-course/{courseId}")
    @Transactional
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId) {
        try {
            System.out.println("Received request to delete course with ID: " + courseId);
            
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final User currentUser = getUserFromAuthentication(auth);
            
            System.out.println("User attempting to delete course: " + currentUser.getEmail());
            
            // Get the course
            Course course = courseService.getCourseById(courseId);
            System.out.println("Found course: " + course.getName() + " (ID: " + course.getId() + ")");
            System.out.println("Course teacher ID: " + course.getTeacher().getId());
            System.out.println("Current user ID: " + currentUser.getId());
            
            // Verify the current user is the teacher of this course
            if (!course.getTeacher().getId().equals(currentUser.getId())) {
                System.out.println("Authorization failed: User is not the course teacher");
                return ResponseEntity.status(403).body("You are not authorized to delete this course");
            }
            
            // Delete all course memberships
            List<CourseMembership> memberships = courseMembershipService.getMembershipsByCourse(course);
            System.out.println("Deleting " + memberships.size() + " course memberships");
            
            for (CourseMembership membership : memberships) {
                System.out.println("Deleting membership ID: " + membership.getId());
                courseMembershipService.removeMembership(membership.getId());
            }
            
            // Delete the course
            System.out.println("Now deleting the course itself");
            courseService.deleteCourse(courseId);
            
            System.out.println("Course deletion completed successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error deleting course: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting course: " + e.getMessage());
        }
    }

    @GetMapping("/archived-courses")
    public ResponseEntity<List<CourseDTO>> getArchivedCourses() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final User currentUser = getUserFromAuthentication(auth);
        
        List<Course> archivedCourses = courseService.getArchivedCoursesByTeacher(currentUser);
        List<CourseDTO> courseDTOs = archivedCourses.stream()
                .map(CourseDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(courseDTOs);
    }
    
    @GetMapping("/active-courses")
    public ResponseEntity<List<CourseDTO>> getActiveCourses() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final User currentUser = getUserFromAuthentication(auth);
        
        List<Course> activeCourses = courseService.getActiveCoursesByTeacher(currentUser);
        List<CourseDTO> courseDTOs = activeCourses.stream()
                .map(CourseDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(courseDTOs);
    }
    
    @PutMapping("/archive-course/{courseId}")
    @Transactional
    public ResponseEntity<?> archiveCourse(@PathVariable Long courseId) {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final User currentUser = getUserFromAuthentication(auth);
            
            // Get the course
            Course course = courseService.getCourseById(courseId);
            
            // Verify the current user is the teacher of this course
            if (!course.getTeacher().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You are not authorized to archive this course");
            }
            
            // Archive the course
            course.setArchived(true);
            courseService.createCourse(course); // This will update the existing course
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error archiving course: " + e.getMessage());
        }
    }
    
    @PutMapping("/unarchive-course/{courseId}")
    @Transactional
    public ResponseEntity<?> unarchiveCourse(@PathVariable Long courseId) {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final User currentUser = getUserFromAuthentication(auth);
            
            // Get the course
            Course course = courseService.getCourseById(courseId);
            
            // Verify the current user is the teacher of this course
            if (!course.getTeacher().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("You are not authorized to unarchive this course");
            }
            
            // Unarchive the course
            course.setArchived(false);
            courseService.createCourse(course); // This will update the existing course
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error unarchiving course: " + e.getMessage());
        }
    }
    
    @GetMapping("/course/{courseId}")
    public ResponseEntity<CourseDTO> getCourseDetails(@PathVariable Long courseId) {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final User currentUser = getUserFromAuthentication(auth);
            
            // Get the course
            Course course = courseService.getCourseById(courseId);
            
            // Verify the current user is the teacher of this course or a member
            if (!course.getTeacher().getId().equals(currentUser.getId())) {
                // Check if the user is a member of the course
                boolean isMember = courseMembershipService.getMembershipsByUser(currentUser)
                    .stream()
                    .anyMatch(membership -> membership.getCourse().getId().equals(courseId));
                
                if (!isMember) {
                    return ResponseEntity.status(403).body(null);
                }
            }
            
            CourseDTO courseDTO = CourseDTO.fromEntity(course);
            return ResponseEntity.ok(courseDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}