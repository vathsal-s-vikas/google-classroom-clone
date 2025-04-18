package com.classroom.repository;

import com.classroom.model.CourseMembership;
import com.classroom.model.Course;
import com.classroom.model.User;
import com.classroom.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseMembershipRepository extends JpaRepository<CourseMembership, Long> {
    List<CourseMembership> findByCourse(Course course);  // Fetch memberships by course
    List<CourseMembership> findByUser(User user);  // Fetch memberships by user
    List<CourseMembership> findByCourseAndRole(Course course, UserType role);  // Fetch memberships by role
    List<CourseMembership> findByUserAndRole(User user, UserType role);  // Fetch memberships by user and role
    List<CourseMembership> findByUserAndCourse(User user, Course course);  // Fetch memberships by user and course
    List<CourseMembership> findByUserAndCourseAndRole(User user, Course course, UserType role);  // Fetch memberships by user, course, and role

    /**
     * Find all students in a course
     */
    @Query("SELECT cm.user FROM CourseMembership cm WHERE cm.course = :course AND cm.role = 'STUDENT'")
    List<User> findStudentsByCourse(@Param("course") Course course);
}