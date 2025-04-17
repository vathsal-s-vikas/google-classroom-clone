package com.classroom.repository;

import com.classroom.model.Assignment;
import com.classroom.model.AssignmentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, Long> {
    List<AssignmentAttachment> findByAssignment(Assignment assignment);
} 