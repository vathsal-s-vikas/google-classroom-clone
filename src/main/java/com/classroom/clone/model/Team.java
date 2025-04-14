package com.classroom.clone.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "teams")
@Getter
@Setter
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String projectTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private GroupAssignment assignment;

    @ManyToMany
    @JoinTable(
            name = "team_memberships",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private List<User> members = new ArrayList<>();

    @OneToOne(mappedBy = "team")
    private Submission submission;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Adds a member to the team if they are not already on the team
     * and if the team is not full.
     *
     * @param student The student to add to the team
     * @return true if the student was added, false otherwise
     */
    public boolean addMember(User student) {
        if (student == null || !student.getUserType().equals(User.UserType.STUDENT)) {
            return false;
        }

        // Check if the student is already a member
        if (members.contains(student)) {
            return false;
        }

        // Check if team is full
        if (isFull()) {
            return false;
        }

        return members.add(student);
    }

    /**
     * Removes a member from the team.
     *
     * @param student The student to remove
     * @return true if the student was removed, false if they weren't a member
     */
    public boolean removeMember(User student) {
        if (student == null) {
            return false;
        }

        // Can't remove the creator if they're the only member
        if (student.equals(createdBy) && members.size() <= 1) {
            return false;
        }

        return members.remove(student);
    }

    /**
     * Checks if the team is full based on the maximum team size.
     *
     * @return true if the team has reached its maximum capacity
     */
    public boolean isFull() {
        return assignment != null && members.size() >= assignment.getMaxTeamSize();
    }

    /**
     * Checks if the team meets the minimum size requirement.
     *
     * @return true if the team has at least the minimum required members
     */
    public boolean meetsMinimumSize() {
        return assignment != null && members.size() >= assignment.getMinTeamSize();
    }

    /**
     * Gets the number of open spots remaining on the team.
     *
     * @return the number of available spots or 0 if team is full
     */
    public int getAvailableSpots() {
        if (assignment == null) {
            return 0;
        }

        int openSpots = assignment.getMaxTeamSize() - members.size();
        return Math.max(0, openSpots);
    }

    /**
     * Checks if a student is a member of this team.
     *
     * @param studentId the ID of the student to check
     * @return true if the student is a member of this team
     */
    public boolean hasMember(Long studentId) {
        if (studentId == null) {
            return false;
        }

        return members.stream()
                .anyMatch(member -> member.getId().equals(studentId));
    }

    /**
     * Gets the team leader (creator of the team).
     *
     * @return the team leader
     */
    public User getTeamLeader() {
        return createdBy;
    }

    /**
     * Sets a project title if required by the assignment.
     *
     * @param title the project title to set
     * @return true if title was set, false if not required or already exists
     */
    public boolean setProjectTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }

        if (assignment != null && assignment.isRequireProjectTitle()) {
            // Check if title is available
            if (assignment.isProjectTitleAvailable(title)) {
                this.projectTitle = title;
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the submission status for this team.
     *
     * @return Optional containing the submission if one exists
     */
    public Optional<Submission> getSubmission() {
        return Optional.ofNullable(submission);
    }

    /**
     * Checks if the team has submitted the assignment.
     *
     * @return true if the team has submitted
     */
    public boolean hasSubmitted() {
        return submission != null;
    }

    /**
     * Checks if all team members have confirmed their membership.
     * This is a placeholder that would need implementation based on your requirements.
     *
     * @return true if all members have confirmed
     */
    public boolean allMembersConfirmed() {
        // Implementation would depend on how you track member confirmation
        // This is just a placeholder
        return true;
    }
}