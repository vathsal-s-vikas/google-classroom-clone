package com.classroom.service;

import com.classroom.model.Assignment;
import com.classroom.model.Course;
import com.classroom.repository.AssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    public List<Assignment> getAssignmentsByCourse(Course course) {
        return assignmentRepository.findByCourse(course);
    }
    
    public Assignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id " + id));
    }
    
    public Assignment createAssignment(Assignment assignment) {
        return assignmentRepository.save(assignment);
    }
    
    public Assignment updateAssignment(Assignment assignment) {
        // Ensure the assignment exists
        if (!assignmentRepository.existsById(assignment.getId())) {
            throw new RuntimeException("Assignment not found with id " + assignment.getId());
        }
        return assignmentRepository.save(assignment);
    }
    
    public void deleteAssignment(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new RuntimeException("Assignment not found with id " + id);
        }
        assignmentRepository.deleteById(id);
    }
} 