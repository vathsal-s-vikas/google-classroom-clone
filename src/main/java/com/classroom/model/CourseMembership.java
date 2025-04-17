package com.classroom.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"course_id","user_id"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CourseMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"memberships", "contents", "assignments", "teacher"})
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"taughtCourses", "courseMemberships", "uploadedContents", "createdTeams", "teamMemberships",
                        "individualSubmissions", "evaluatedSubmissions", "notifications", "assignedStudents", 
                        "assignedTA", "permissions", "password"})
    private User user;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType role;


    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserType getRole() {
        return role;
    }

    public void setRole(UserType role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

//    @ManyToMany
//    @JoinTable(
//            name = "course_offering_students",
//            joinColumns = @JoinColumn(name = "course_offering_id"),
//            inverseJoinColumns = @JoinColumn(name = "student_id")
//    )
//    private Set<Student> enrolledStudents = new HashSet<>();
//
//    // 🔗 TAs assisting in this offering
//    @OneToMany(mappedBy = "CourseOffering", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<TA> teachingAssistants = new HashSet<>();


}
