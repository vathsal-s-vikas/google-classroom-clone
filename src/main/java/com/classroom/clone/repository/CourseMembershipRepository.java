package com.classroom.clone.repository;

import com.classroom.clone.model.Course;
import com.classroom.clone.model.CourseMembership;
import com.classroom.clone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseMembershipRepository extends JpaRepository<CourseMembership, Integer> {

    List<CourseMembership> findByCourse(Course course);

    List<CourseMembership> findByUser(User user);

    Optional<CourseMembership> findByCourseAndUser(Course course, User user);

    @Query("SELECT cm FROM CourseMembership cm WHERE cm.course = :course AND cm.role = :role")
    List<CourseMembership> findByCourseAndRole(@Param("course") Course course, @Param("role") String role);

    @Query("SELECT COUNT(cm) FROM CourseMembership cm WHERE cm.course = :course AND cm.role = 'STUDENT'")
    int countStudentsInCourse(@Param("course") Course course);

    void deleteByCourseAndUser(Course course, User user);
}