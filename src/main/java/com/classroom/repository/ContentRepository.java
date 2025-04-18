package com.classroom.repository;

import com.classroom.model.Content;
import com.classroom.model.Content.ContentType;
import com.classroom.model.Content.ContentVisibility;
import com.classroom.model.Course;
import com.classroom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    
    /**
     * Find all content for a specific course
     */
    List<Content> findByCourse(Course course);
    
    /**
     * Find all content for a specific course with a given visibility
     */
    List<Content> findByCourseAndVisibility(Course course, ContentVisibility visibility);
    
    /**
     * Find all content uploaded by a specific user
     */
    List<Content> findByUploader(User uploader);
    
    /**
     * Find all content of a specific type
     */
    List<Content> findByContentType(ContentType contentType);
    
    /**
     * Find all visible content for a course
     */
    @Query("SELECT c FROM Content c WHERE c.course = :course AND c.visibility = 'VISIBLE'")
    List<Content> findVisibleContentByCourse(@Param("course") Course course);
    
    /**
     * Find all scheduled content for a course that should now be visible
     */
    @Query("SELECT c FROM Content c WHERE c.course = :course AND c.visibility = 'SCHEDULED' AND c.scheduledFor <= :now")
    List<Content> findScheduledContentDueByCourse(@Param("course") Course course, @Param("now") LocalDateTime now);
    
    /**
     * Count content by course
     */
    long countByCourse(Course course);
    
    /**
     * Count content by type for a specific course
     */
    long countByCourseAndContentType(Course course, ContentType contentType);
    
    /**
     * Find recent content for a course
     */
    List<Content> findByCourseOrderByCreatedAtDesc(Course course);
    
    /**
     * Find content created after a specific date
     */
    List<Content> findByCourseAndCreatedAtAfter(Course course, LocalDateTime date);
} 