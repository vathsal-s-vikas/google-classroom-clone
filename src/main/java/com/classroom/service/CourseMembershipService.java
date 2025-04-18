package com.classroom.service;

import com.classroom.model.CourseMembership;
import com.classroom.model.Course;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.CourseMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseMembershipService {

    @Autowired
    private CourseMembershipRepository courseMembershipRepository;

    public List<CourseMembership> getMembershipsByCourse(Course course) {
        return courseMembershipRepository.findByCourse(course);
    }

    public List<CourseMembership> getMembershipsByUser(User user) {
        return courseMembershipRepository.findByUser(user);
    }

    public CourseMembership addMembership(CourseMembership membership) {
        return courseMembershipRepository.save(membership);
    }

    public void removeMembership(Long id) {
        courseMembershipRepository.deleteById(id);
    }
    
    /**
     * Get course memberships for a specific course and role
     */
    public List<CourseMembership> getCourseMembershipsByCourseAndRole(Course course, UserType role) {
        return courseMembershipRepository.findByCourseAndRole(course, role);
    }
    
    /**
     * Get all courses where a user has a specific role
     */
    public List<Course> getCoursesForUserWithRole(User user, UserType role) {
        return courseMembershipRepository.findByUserAndRole(user, role)
                .stream()
                .map(CourseMembership::getCourse)
                .collect(Collectors.toList());
    }
}