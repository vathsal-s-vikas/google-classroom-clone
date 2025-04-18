package com.classroom.repository;

import com.classroom.model.Mark;
import com.classroom.model.MarkHistory;
import com.classroom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MarkHistoryRepository extends JpaRepository<MarkHistory, Long> {
    
    /**
     * Find all history records for a specific mark
     */
    List<MarkHistory> findByMark(Mark mark);
    
    /**
     * Find all changes made by a specific user
     */
    List<MarkHistory> findByChangedBy(User changedBy);
    
    /**
     * Find all changes made after a specific date
     */
    List<MarkHistory> findByChangedAtAfter(LocalDateTime date);
    
    /**
     * Find all changes made between two dates
     */
    List<MarkHistory> findByChangedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Count the number of changes for a specific mark
     */
    long countByMark(Mark mark);
} 