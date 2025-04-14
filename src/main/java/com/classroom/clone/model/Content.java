package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contents")
@Data
@NoArgsConstructor
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @Column(name = "content_data", columnDefinition = "JSON")
    private String contentData;

    @Column(name = "resource_url")
    private String resourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentVisibility visibility = ContentVisibility.VISIBLE;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if the content is visible to students at the current time
     * @return true if the content is visible
     */
    public boolean isVisibleNow() {
        if (visibility == ContentVisibility.HIDDEN) {
            return false;
        }

        if (visibility == ContentVisibility.SCHEDULED) {
            return scheduledFor != null && LocalDateTime.now().isAfter(scheduledFor);
        }

        return true; // VISIBLE
    }

    /**
     * Gets the status of the content as a user-friendly string
     * @return status string
     */
    public String getStatusString() {
        switch (visibility) {
            case VISIBLE:
                return "Published";
            case HIDDEN:
                return "Hidden";
            case SCHEDULED:
                if (scheduledFor == null) {
                    return "Scheduled (no date)";
                }
                return "Scheduled for " + scheduledFor.toString();
            default:
                return "Unknown";
        }
    }

    /**
     * Gets a display-friendly content type name
     * @return formatted content type name
     */
    public String getContentTypeName() {
        if (contentType == null) {
            return "Unknown";
        }

        return contentType.toString().charAt(0) +
                contentType.toString().substring(1).toLowerCase().replace("_", " ");
    }

    /**
     * Checks if this content has embedded data
     * @return true if content has data stored directly in database
     */
    public boolean hasContentData() {
        return contentData != null && !contentData.isEmpty();
    }

    /**
     * Checks if this content links to an external resource
     * @return true if content has a resource URL
     */
    public boolean hasResourceUrl() {
        return resourceUrl != null && !resourceUrl.isEmpty();
    }

    /**
     * Builds a preview snippet from the content description
     * @param maxLength maximum length of snippet
     * @return preview snippet
     */
    public String getPreviewSnippet(int maxLength) {
        if (description == null || description.isEmpty()) {
            return "";
        }

        if (description.length() <= maxLength) {
            return description;
        }

        return description.substring(0, maxLength) + "...";
    }
}