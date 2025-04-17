package com.classroom.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;


@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;


    private boolean googleLinked;

    private String oauthId;

    @JsonIgnore
    private String password;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    private boolean active;


    // Bidirectional relationships
    @OneToMany(mappedBy = "teacher")
    @JsonIgnore
    private Set<Course> taughtCourses = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private Set<CourseMembership> courseMemberships = new HashSet<>();

    @OneToMany(mappedBy = "uploader")
    @JsonIgnore
    private Set<Content> uploadedContents = new HashSet<>();

    @OneToMany(mappedBy = "createdBy")
    @JsonIgnore
    private Set<Team> createdTeams = new HashSet<>();

    @OneToMany(mappedBy = "student")
    @JsonIgnore
    private Set<TeamMembership> teamMemberships = new HashSet<>();

    @OneToMany(mappedBy = "student")
    @JsonIgnore
    private Set<Submission> individualSubmissions = new HashSet<>();

    @OneToMany(mappedBy = "evaluator")
    @JsonIgnore
    private Set<Mark> evaluatedSubmissions = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "ta")
    @JsonIgnore
    private Set<TAStudentAssignment> assignedStudents = new HashSet<>();

    @OneToMany(mappedBy = "student")
    @JsonIgnore
    private Set<TAStudentAssignment> assignedTA = new HashSet<>();

    @OneToMany(mappedBy = "ta")
    @JsonIgnore
    private Set<PermissionSettings> permissions = new HashSet<>();

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Course> getTaughtCourses() {
        return taughtCourses;
    }

    public void setTaughtCourses(Set<Course> taughtCourses) {
        this.taughtCourses = taughtCourses;
    }

    public Set<CourseMembership> getCourseMemberships() {
        return courseMemberships;
    }

    public void setCourseMemberships(Set<CourseMembership> courseMemberships) {
        this.courseMemberships = courseMemberships;
    }

    public Set<Content> getUploadedContents() {
        return uploadedContents;
    }

    public void setUploadedContents(Set<Content> uploadedContents) {
        this.uploadedContents = uploadedContents;
    }

    public Set<Team> getCreatedTeams() {
        return createdTeams;
    }

    public void setCreatedTeams(Set<Team> createdTeams) {
        this.createdTeams = createdTeams;
    }

    public Set<TeamMembership> getTeamMemberships() {
        return teamMemberships;
    }

    public void setTeamMemberships(Set<TeamMembership> teamMemberships) {
        this.teamMemberships = teamMemberships;
    }

    public Set<Submission> getIndividualSubmissions() {
        return individualSubmissions;
    }

    public void setIndividualSubmissions(Set<Submission> individualSubmissions) {
        this.individualSubmissions = individualSubmissions;
    }

    public Set<Mark> getEvaluatedSubmissions() {
        return evaluatedSubmissions;
    }

    public void setEvaluatedSubmissions(Set<Mark> evaluatedSubmissions) {
        this.evaluatedSubmissions = evaluatedSubmissions;
    }

    public Set<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<Notification> notifications) {
        this.notifications = notifications;
    }

    public Set<TAStudentAssignment> getAssignedStudents() {
        return assignedStudents;
    }

    public void setAssignedStudents(Set<TAStudentAssignment> assignedStudents) {
        this.assignedStudents = assignedStudents;
    }

    public Set<TAStudentAssignment> getAssignedTA() {
        return assignedTA;
    }

    public void setAssignedTA(Set<TAStudentAssignment> assignedTA) {
        this.assignedTA = assignedTA;
    }

    public Set<PermissionSettings> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionSettings> permissions) {
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isGoogleLinked() {
        return googleLinked;
    }

    public void setGoogleLinked(boolean googleLinked) {
        this.googleLinked = googleLinked;
    }

    public String getOauthId() {
        return oauthId;
    }

    public void setOauthId(String oauthId) {
        this.oauthId = oauthId;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }



}
