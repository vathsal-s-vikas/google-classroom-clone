package com.classroom.service;

import com.classroom.model.Content;
import com.classroom.model.Content.ContentType;
import com.classroom.model.Content.ContentVisibility;
import com.classroom.model.Course;
import com.classroom.model.User;
import com.classroom.repository.ContentRepository;
import com.classroom.repository.CourseMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class ContentService {

    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private CourseMembershipRepository courseMembershipRepository;

    /**
     * Get all content for a course
     */
    public List<Content> getContentByCourse(Course course) {
        return contentRepository.findByCourse(course);
    }
    
    /**
     * Get all visible content for a course
     */
    public List<Content> getVisibleContentByCourse(Course course) {
        List<Content> visibleContent = contentRepository.findVisibleContentByCourse(course);
        
        // Also include scheduled content that is now due
        List<Content> dueScheduledContent = contentRepository.findScheduledContentDueByCourse(course, LocalDateTime.now());
        for (Content content : dueScheduledContent) {
            content.setVisibility(ContentVisibility.VISIBLE);
            contentRepository.save(content);
        }
        
        visibleContent.addAll(dueScheduledContent);
        return visibleContent;
    }
    
    /**
     * Get content by id
     */
    public Optional<Content> getContentById(Long id) {
        return contentRepository.findById(id);
    }
    
    /**
     * Helper method to send notifications about new content
     */
    private void sendContentNotifications(Content content) {
        try {
            // Get all students in the course
            List<User> students = courseMembershipRepository.findStudentsByCourse(content.getCourse());
            
            if (!students.isEmpty() && notificationService != null) {
                notificationService.createCourseContentNotification(content, students);
            }
        } catch (Exception e) {
            // Log error but continue - notification failure shouldn't prevent content creation
            System.err.println("Failed to create notifications for new content: " + e.getMessage());
        }
    }
    
    /**
     * Create announcement content
     */
    @Transactional
    public Content createAnnouncement(Course course, User uploader, String title, 
                                      String description, ContentVisibility visibility,
                                      LocalDateTime scheduledFor) {
        Content content = new Content();
        content.setCourse(course);
        content.setTitle(title);
        content.setDescription(description);
        content.setContentType(ContentType.ANNOUNCEMENT);
        content.setVisibility(visibility);
        content.setScheduledFor(scheduledFor);
        content.setUploader(uploader);
        
        Content savedContent = contentRepository.save(content);
        
        // Create notifications if content is immediately visible
        if (visibility == ContentVisibility.VISIBLE) {
            sendContentNotifications(savedContent);
        }
        
        return savedContent;
    }
    
    /**
     * Create document content
     */
    @Transactional
    public Content createDocument(Course course, User uploader, String title,
                                 String description, MultipartFile file,
                                 ContentVisibility visibility, LocalDateTime scheduledFor) throws IOException {
        Content content = new Content();
        content.setCourse(course);
        content.setTitle(title);
        content.setDescription(description);
        content.setContentType(ContentType.DOCUMENT);
        content.setContentData(Base64.getEncoder().encodeToString(file.getBytes()));
        content.setVisibility(visibility);
        content.setScheduledFor(scheduledFor);
        content.setUploader(uploader);
        
        Content savedContent = contentRepository.save(content);
        
        // Create notifications if content is immediately visible
        if (visibility == ContentVisibility.VISIBLE) {
            sendContentNotifications(savedContent);
        }
        
        return savedContent;
    }
    
    /**
     * Create link content
     */
    @Transactional
    public Content createLink(Course course, User uploader, String title,
                             String description, String resourceUrl,
                             ContentVisibility visibility, LocalDateTime scheduledFor) {
        Content content = new Content();
        content.setCourse(course);
        content.setTitle(title);
        content.setDescription(description);
        content.setContentType(ContentType.LINK);
        content.setResourceUrl(resourceUrl);
        content.setVisibility(visibility);
        content.setScheduledFor(scheduledFor);
        content.setUploader(uploader);
        
        Content savedContent = contentRepository.save(content);
        
        // Create notifications if content is immediately visible
        if (visibility == ContentVisibility.VISIBLE) {
            sendContentNotifications(savedContent);
        }
        
        return savedContent;
    }
    
    /**
     * Create video content
     */
    @Transactional
    public Content createVideo(Course course, User uploader, String title,
                              String description, String videoUrl,
                              ContentVisibility visibility, LocalDateTime scheduledFor) {
        Content content = new Content();
        content.setCourse(course);
        content.setTitle(title);
        content.setDescription(description);
        content.setContentType(ContentType.VIDEO);
        content.setResourceUrl(videoUrl);
        content.setVisibility(visibility);
        content.setScheduledFor(scheduledFor);
        content.setUploader(uploader);
        
        Content savedContent = contentRepository.save(content);
        
        // Create notifications if content is immediately visible
        if (visibility == ContentVisibility.VISIBLE) {
            sendContentNotifications(savedContent);
        }
        
        return savedContent;
    }
    
    /**
     * Update content
     */
    @Transactional
    public Content updateContent(Long contentId, String title, String description,
                                String resourceUrl, ContentVisibility visibility,
                                LocalDateTime scheduledFor) {
        Optional<Content> contentOpt = contentRepository.findById(contentId);
        if (contentOpt.isPresent()) {
            Content content = contentOpt.get();
            
            if (title != null) content.setTitle(title);
            if (description != null) content.setDescription(description);
            if (resourceUrl != null && (content.getContentType() == ContentType.LINK || 
                                       content.getContentType() == ContentType.VIDEO)) {
                content.setResourceUrl(resourceUrl);
            }
            if (visibility != null) content.setVisibility(visibility);
            if (scheduledFor != null) content.setScheduledFor(scheduledFor);
            
            return contentRepository.save(content);
        }
        return null;
    }
    
    /**
     * Update document content with new file
     */
    @Transactional
    public Content updateDocumentContent(Long contentId, String title, String description,
                                        MultipartFile file, ContentVisibility visibility,
                                        LocalDateTime scheduledFor) throws IOException {
        Optional<Content> contentOpt = contentRepository.findById(contentId);
        if (contentOpt.isPresent()) {
            Content content = contentOpt.get();
            
            if (content.getContentType() != ContentType.DOCUMENT) {
                throw new IllegalArgumentException("Content is not a document");
            }
            
            if (title != null) content.setTitle(title);
            if (description != null) content.setDescription(description);
            if (file != null) content.setContentData(Base64.getEncoder().encodeToString(file.getBytes()));
            if (visibility != null) content.setVisibility(visibility);
            if (scheduledFor != null) content.setScheduledFor(scheduledFor);
            
            return contentRepository.save(content);
        }
        return null;
    }
    
    /**
     * Delete content
     */
    @Transactional
    public void deleteContent(Long contentId) {
        contentRepository.deleteById(contentId);
    }
    
    /**
     * Get recent content for a course
     */
    public List<Content> getRecentContentByCourse(Course course) {
        return contentRepository.findByCourseOrderByCreatedAtDesc(course);
    }
    
    /**
     * Count content by course
     */
    public long countContentByCourse(Course course) {
        return contentRepository.countByCourse(course);
    }
    
    /**
     * Count content by type for a course
     */
    public long countContentByTypeForCourse(Course course, ContentType contentType) {
        return contentRepository.countByCourseAndContentType(course, contentType);
    }
} 