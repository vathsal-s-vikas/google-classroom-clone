package com.classroom.repository;

import com.classroom.model.Team;
import com.classroom.model.TeamMembership;
import com.classroom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {
    
    /**
     * Find all memberships for a specific team
     */
    List<TeamMembership> findByTeam(Team team);
    
    /**
     * Find all team memberships for a student
     */
    List<TeamMembership> findByStudent(User student);
    
    /**
     * Find a specific membership by team and student
     */
    TeamMembership findByTeamAndStudent(Team team, User student);
    
    /**
     * Count members in a team
     */
    long countByTeam(Team team);
    
    /**
     * Check if a student is already in any team for a specific assignment
     */
    @Query("SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TeamMembership tm " +
           "WHERE tm.student = :student AND tm.team.assignment.id = :assignmentId")
    boolean isStudentInAnyTeamForAssignment(@Param("student") User student, @Param("assignmentId") Long assignmentId);
    
    /**
     * Find all memberships for teams in a specific assignment
     */
    @Query("SELECT tm FROM TeamMembership tm WHERE tm.team.assignment.id = :assignmentId")
    List<TeamMembership> findMembershipsByAssignment(@Param("assignmentId") Long assignmentId);
    
    /**
     * Find all accepted memberships for a team
     */
    List<TeamMembership> findByTeamAndAcceptedTrue(Team team);
    
    /**
     * Find all pending invitations for a student
     */
    List<TeamMembership> findByStudentAndAcceptedFalse(User student);
    
    /**
     * Count accepted members in a team
     */
    long countByTeamAndAcceptedTrue(Team team);
    
    /**
     * Find all teams where the user is a leader
     */
    List<TeamMembership> findByStudentAndLeaderTrue(User student);
    
    /**
     * Find the team leader for a specific team
     */
    Optional<TeamMembership> findByTeamAndLeaderTrue(Team team);
} 