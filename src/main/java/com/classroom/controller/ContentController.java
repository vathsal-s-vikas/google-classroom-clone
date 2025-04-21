package com.classroom.controller;

import com.classroom.model.Content;
import com.classroom.model.Content.ContentType;
import com.classroom.model.Content.ContentVisibility;
import com.classroom.model.Course;
import com.classroom.model.User;
import com.classroom.model.CourseMembership;
import com.classroom.model.UserType;
import com.classroom.service.ContentService;
import com.classroom.service.CourseService;
import com.classroom.repository.UserRepository;
import com.classroom.repository.CourseMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    @Autowired
    private ContentService contentService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseMembershipRepository courseMembershipRepository;
    
    /**
     * Get current authenticated user from OAuth2 authentication
     */
    private User getCurrentUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
    
    /**
     * Check if a user is a member of a course
     */
    private boolean isUserMemberOfCourse(User user, Course course) {
        return !courseMembershipRepository.findByUserAndCourse(user, course).isEmpty();
    }
    
    /**
     * Check if a user is a teacher of a course
     */
    private boolean isUserTeacherOfCourse(User user, Course course) {
        List<CourseMembership> memberships = courseMembershipRepository.findByUserAndCourseAndRole(user, course, UserType.TEACHER);
        return !memberships.isEmpty() || course.getTeacher().equals(user);
    }
    
    /**
     * Get all content for a course
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getContentByCourse(@PathVariable Long courseId,
                                              @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        Course course = courseService.getCourseById(courseId);
        
        // Check if the user is a member of the course
        if (!isUserMemberOfCourse(currentUser, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not a member of this course"));
        }
        
        // If user is a teacher, return all content
        if (isUserTeacherOfCourse(currentUser, course)) {
            return ResponseEntity.ok(contentService.getContentByCourse(course));
        } else {
            // If user is a student, return only visible content
            return ResponseEntity.ok(contentService.getVisibleContentByCourse(course));
        }
    }
    
    /**
     * Get content by ID
     */
    @GetMapping("/{contentId}")
    public ResponseEntity<?> getContentById(@PathVariable Long contentId,
                                          @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        Optional<Content> contentOpt = contentService.getContentById(contentId);
        
        if (!contentOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Content not found"));
        }
        
        Content content = contentOpt.get();
        Course course = content.getCourse();
        
        // Check if the user is a member of the course
        if (!isUserMemberOfCourse(currentUser, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not a member of this course"));
        }
        
        // If content is not visible and user is not a teacher, don't allow access
        if (content.getVisibility() != ContentVisibility.VISIBLE && 
            !isUserTeacherOfCourse(currentUser, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "This content is not available"));
        }
        
        return ResponseEntity.ok(content);
    }
    
    /**
     * Create content (general endpoint)
     */
    @PostMapping
    public ResponseEntity<?> createContent(
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String link,
            @RequestParam(required = false) MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        Course course = courseService.getCourseById(courseId);
        
        // Check if user is a teacher of the course
        if (!isUserTeacherOfCourse(currentUser, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only teachers can create content"));
        }
        
        try {
            Content content;
            ContentType contentType = ContentType.valueOf(type.toUpperCase());
            ContentVisibility visibility = ContentVisibility.VISIBLE; // Default to visible
            
            switch (contentType) {
                case DOCUMENT:
                    if (file == null) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "File is required for document content"));
                    }
                    content = contentService.createDocument(
                            course, currentUser, title, description, file, visibility, null);
                    break;
                case LINK:
                    if (link == null || link.isEmpty()) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Link URL is required for link content"));
                    }
                    content = contentService.createLink(
                            course, currentUser, title, description, link, visibility, null);
                    break;
                case ANNOUNCEMENT:
                    content = contentService.createAnnouncement(
                            course, currentUser, title, description, visibility, null);
                    break;
                case VIDEO:
                    if (link == null || link.isEmpty()) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Video URL is required for video content"));
                    }
                    content = contentService.createVideo(
                            course, currentUser, title, description, link, visibility, null);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Unsupported content type: " + type));
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(content);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid content type: " + e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create content: " + e.getMessage()));
        }
    }
    
    /**
     * Update content (general endpoint)
     */
    @PutMapping
    public ResponseEntity<?> updateContent(
            @RequestParam(required = false) Long id,
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String link,
            @RequestParam(required = false) MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal) {
        
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content ID is required for updates"));
        }
        
        User currentUser = getCurrentUser(principal);
        Course course = courseService.getCourseById(courseId);
        
        // Check if user is a teacher of the course
        if (!isUserTeacherOfCourse(currentUser, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only teachers can update content"));
        }
        
        try {
            Optional<Content> contentOpt = contentService.getContentById(id);
            if (!contentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Content not found with ID: " + id));
            }
            
            Content existingContent = contentOpt.get();
            
            // Check if the content belongs to the specified course
            if (!existingContent.getCourse().getId().equals(courseId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Content does not belong to the specified course"));
            }
            
            // Update content based on its type
            ContentType contentType = ContentType.valueOf(type.toUpperCase());
            Content updatedContent;
            
            switch (contentType) {
                case DOCUMENT:
                    updatedContent = contentService.updateDocumentContent(
                            id, title, description, file, null, null);
                    break;
                case LINK:
                    updatedContent = contentService.updateContent(
                            id, title, description, link, null, null);
                    break;
                case ANNOUNCEMENT:
                    updatedContent = contentService.updateContent(
                            id, title, description, null, null, null);
                    break;
                case VIDEO:
                    updatedContent = contentService.updateContent(
                            id, title, description, link, null, null);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Unsupported content type: " + type));
            }
            
            return ResponseEntity.ok(updatedContent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid content type: " + e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update content: " + e.getMessage()));
        }
    }
    
    /**
     * Delete content
     */
    @DeleteMapping("/{contentId}")
    public ResponseEntity<?> deleteContent(
            @PathVariable Long contentId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        Optional<Content> contentOpt = contentService.getContentById(contentId);
        
        if (!contentOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Content not found"));
        }
        
        Content content = contentOpt.get();
        Course course = content.getCourse();
        
        // Check if user is the uploader or a teacher of the course
        if (!content.getUploader().equals(currentUser) && 
            !isUserTeacherOfCourse(currentUser, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have permission to delete this content"));
        }
        
        try {
            contentService.deleteContent(contentId);
            return ResponseEntity.ok(Map.of("message", "Content deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete content: " + e.getMessage()));
        }
    }
    
    /**
     * Get count of content by type for a course
     */
    @GetMapping("/count/course/{courseId}")
    public ResponseEntity<?> getContentCountByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = getCurrentUser(principal);
        Course course = courseService.getCourseById(courseId);
        
        // Check if user is a member of the course
        if (!isUserMemberOfCourse(currentUser, course)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not a member of this course"));
        }
        
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", contentService.countContentByCourse(course));
        counts.put("announcements", contentService.countContentByTypeForCourse(course, ContentType.ANNOUNCEMENT));
        counts.put("documents", contentService.countContentByTypeForCourse(course, ContentType.DOCUMENT));
        counts.put("links", contentService.countContentByTypeForCourse(course, ContentType.LINK));
        counts.put("videos", contentService.countContentByTypeForCourse(course, ContentType.VIDEO));
        
        return ResponseEntity.ok(counts);
    }
}
