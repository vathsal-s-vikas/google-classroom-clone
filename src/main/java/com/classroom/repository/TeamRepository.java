package com.classroom.repository;

import com.classroom.model.Assignment;
import com.classroom.model.Course;
import com.classroom.model.GroupAssignment;
import com.classroom.model.Team;
import com.classroom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    /**
     * Find all teams for a specific assignment
     */
    List<Team> findByAssignment(GroupAssignment assignment);
    
    /**
     * Find a team by its name within an assignment
     */
    Optional<Team> findByAssignmentAndName(GroupAssignment assignment, String name);
    
    /**
     * Find all teams created by a specific user
     */
    List<Team> findByCreatedBy(User user);
    
    /**
     * Count teams for an assignment
     */
    long countByAssignment(GroupAssignment assignment);
    
    /**
     * Find all teams that a student is a member of
     */
    @Query("SELECT t FROM Team t JOIN t.memberships m WHERE m.student = :student AND m.accepted = true")
    List<Team> findTeamsWithStudent(@Param("student") User student);
    
    /**
     * Find a team for a student in a specific assignment
     */
    @Query("SELECT t FROM Team t JOIN t.memberships m WHERE m.student = :student AND t.assignment.id = :assignmentId AND m.accepted = true")
    Team findTeamForStudentAndAssignment(@Param("student") User student, @Param("assignmentId") Long assignmentId);
    
    /**
     * Find all teams for a course
     */
    @Query("SELECT t FROM Team t WHERE t.assignment.assignment.course = :course")
    List<Team> findTeamsByCourse(@Param("course") Course course);

    @Query("SELECT t FROM Team t JOIN TeamMembership m ON t.id = m.team.id " +
           "WHERE m.student.id = :studentId AND m.accepted = true AND t.assignment.id = :assignmentId")
    Optional<Team> findByStudentAndAssignment(@Param("studentId") Long studentId, 
                                              @Param("assignmentId") Long assignmentId);
} 