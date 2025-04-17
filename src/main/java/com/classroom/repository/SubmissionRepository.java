package com.classroom.repository;

import com.classroom.model.Assignment;
import com.classroom.model.Submission;
import com.classroom.model.User;
import com.classroom.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    
    // Find submissions by assignment
    List<Submission> findByAssignment(Assignment assignment);
    
    // Find submission by assignment and student
    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);
    
    // Find submission by assignment and team
    Optional<Submission> findByAssignmentAndTeam(Assignment assignment, Team team);
    
    // Find all submissions by student
    List<Submission> findByStudent(User student);
    
    // Find all submissions by team
    List<Submission> findByTeam(Team team);
    
    // Find all submissions that haven't been evaluated yet
    List<Submission> findByIsEvaluatedFalse();
    
    // Find all submissions for an assignment that haven't been evaluated
    List<Submission> findByAssignmentAndIsEvaluatedFalse(Assignment assignment);
    
    // Count submissions by assignment
    long countByAssignment(Assignment assignment);
} 