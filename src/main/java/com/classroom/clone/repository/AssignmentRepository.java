package com.classroom.clone.repository;

import com.classroom.clone.model.Assignment;
import com.classroom.clone.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {

    List<Assignment> findByCourse(Course course);

    @Query("SELECT a FROM Assignment a WHERE a.course = :course ORDER BY a.deadline ASC")
    List<Assignment> findByCourseOrderByDeadlineAsc(@Param("course") Course course);

    @Query("SELECT a FROM Assignment a WHERE a.deadline > :now ORDER BY a.deadline ASC")
    List<Assignment> findUpcomingAssignments(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Assignment a WHERE a.course = :course AND a.deadline > :now ORDER BY a.deadline ASC")
    List<Assignment> findUpcomingAssignmentsByCourse(@Param("course") Course course, @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Assignment a WHERE a.course IN :courses AND a.deadline > :now ORDER BY a.deadline ASC")
    List<Assignment> findUpcomingAssignmentsByCoursesIn(@Param("courses") List<Course> courses, @Param("now") LocalDateTime now);
}