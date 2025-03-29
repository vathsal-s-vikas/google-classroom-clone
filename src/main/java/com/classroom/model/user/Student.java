package com.classroom.model.user;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Student extends User {
    // Additional student-specific attributes can be added later
}
