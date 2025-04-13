package com.classroom.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "courses")

public class Course {

    @Id
    private String courseCode; // like "CS101", unique

    private String title;

    private String description;

    private String department;


}
