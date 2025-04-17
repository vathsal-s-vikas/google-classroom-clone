package com.classroom.repository;

import com.classroom.model.Course;
import com.classroom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacher(User teacher);  // Fetch courses by teacher
    List<Course> findByTeacherAndIsArchived(User teacher, boolean isArchived);  // Fetch archived courses
}