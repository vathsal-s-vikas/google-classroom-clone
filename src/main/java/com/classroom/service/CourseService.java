package com.classroom.service;

import com.classroom.model.Course;
import com.classroom.model.User;
import com.classroom.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id " + id));
    }

    public List<Course> getCoursesByTeacher(User teacher) {
        return courseRepository.findByTeacher(teacher);
    }

    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    public List<Course> getArchivedCoursesByTeacher(User teacher) {
        return courseRepository.findByTeacherAndIsArchived(teacher, true);
    }
    
    public List<Course> getActiveCoursesByTeacher(User teacher) {
        return courseRepository.findByTeacherAndIsArchived(teacher, false);
    }

    public Course getCourseByInviteCode(String inviteCode) {
        return courseRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Course not found with invite code: " + inviteCode));
    }
}