package com.classroom.clone.repository;

import com.classroom.clone.model.Course;
import com.classroom.clone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    Optional<Course> findByInviteCode(String inviteCode);

    Optional<Course> findByCourseCode(String courseCode);

    List<Course> findByTeacher(User teacher);

    List<Course> findByTeacherAndIsArchived(User teacher, boolean isArchived);

    @Query("SELECT c FROM Course c JOIN c.memberships m WHERE m.user = :user AND m.role = 'STUDENT'")
    List<Course> findCoursesWhereUserIsStudent(@Param("user") User user);

    @Query("SELECT c FROM Course c JOIN c.memberships m WHERE m.user = :user AND m.role = 'TA'")
    List<Course> findCoursesWhereUserIsTA(@Param("user") User user);

    @Query("SELECT c FROM Course c JOIN c.memberships m WHERE m.user = :user")
    List<Course> findAllCoursesByUser(@Param("user") User user);
}