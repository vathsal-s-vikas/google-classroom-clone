package com.classroom.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name ="students")
public class Student extends User{

    @Column(unique = true, nullable = false)
    private String rollNumber;

    private String department;

    private int currentSemester;


//    @ManyToMany
//    @JoinTable(
//            name="student_courses",
//            joinColumns = @JoinColumn(name = "student_id"),
//            inverseJoinColumns = @JoinColumn(name="course_id")
//    )
//    private Set<Course> enrolledCourses = new HashSet<>();
}
