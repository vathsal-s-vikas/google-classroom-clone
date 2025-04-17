package com.classroom.controller;

import com.classroom.model.CourseMembership;
import com.classroom.model.Course;
import com.classroom.model.User;
import com.classroom.service.CourseMembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class CourseMembershipController {

    @Autowired
    private CourseMembershipService courseMembershipService;

    @GetMapping("/course/{courseId}")
    public List<CourseMembership> getMembershipsByCourse(@PathVariable Long courseId) {
        Course course = new Course();
        course.setId(courseId);
        return courseMembershipService.getMembershipsByCourse(course);
    }

    @GetMapping("/user/{userId}")
    public List<CourseMembership> getMembershipsByUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        return courseMembershipService.getMembershipsByUser(user);
    }

    @PostMapping
    public CourseMembership addMembership(@RequestBody CourseMembership membership) {
        return courseMembershipService.addMembership(membership);
    }

    @DeleteMapping("/{id}")
    public void removeMembership(@PathVariable Long id) {
        courseMembershipService.removeMembership(id);
    }
}