package com.classroom.repository;

import com.classroom.model.CourseMembership;
import com.classroom.model.Course;
import com.classroom.model.User;
import com.classroom.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseMembershipRepository extends JpaRepository<CourseMembership, Long> {
    List<CourseMembership> findByCourse(Course course);  // Fetch memberships by course
    List<CourseMembership> findByUser(User user);  // Fetch memberships by user
    List<CourseMembership> findByCourseAndRole(Course course, UserType role);  // Fetch memberships by role
}