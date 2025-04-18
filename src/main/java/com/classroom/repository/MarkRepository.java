package com.classroom.repository;

import com.classroom.model.Assignment;
import com.classroom.model.Mark;
import com.classroom.model.Submission;
import com.classroom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {
    
    /**
     * Find a mark by its associated submission
     */
    Optional<Mark> findBySubmission(Submission submission);
    
    /**
     * Find all marks evaluated by a specific user
     */
    List<Mark> findByEvaluator(User evaluator);
    
    /**
     * Count marks with a specific final score
     */
    long countByFinalMarks(Integer finalMarks);
    
    /**
     * Find marks with a penalty percentage greater than a specified value
     */
    List<Mark> findByPenaltyPercentageGreaterThan(Integer penaltyPercentage);
    
    /**
     * Find all marks for submissions of a specific assignment
     */
    @Query("SELECT m FROM Mark m WHERE m.submission.assignment = :assignment")
    List<Mark> findByAssignment(@Param("assignment") Assignment assignment);
    
    /**
     * Calculate the average final mark for submissions of a specific assignment
     */
    @Query("SELECT AVG(m.finalMarks) FROM Mark m WHERE m.submission.assignment = :assignment")
    Double calculateAverageMarkByAssignment(@Param("assignment") Assignment assignment);
    
    /**
     * Find the highest final mark for submissions of a specific assignment
     */
    @Query("SELECT MAX(m.finalMarks) FROM Mark m WHERE m.submission.assignment = :assignment")
    Integer findHighestMarkByAssignment(@Param("assignment") Assignment assignment);
    
    /**
     * Find the lowest final mark for submissions of a specific assignment
     */
    @Query("SELECT MIN(m.finalMarks) FROM Mark m WHERE m.submission.assignment = :assignment")
    Integer findLowestMarkByAssignment(@Param("assignment") Assignment assignment);
    
    /**
     * Count the number of evaluated submissions for a specific assignment
     */
    @Query("SELECT COUNT(m) FROM Mark m WHERE m.submission.assignment = :assignment")
    Long countByAssignment(@Param("assignment") Assignment assignment);
} 