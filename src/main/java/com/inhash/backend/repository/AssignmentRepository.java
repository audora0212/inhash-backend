package com.inhash.backend.repository;

import com.inhash.backend.domain.Assignment;
import com.inhash.backend.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    List<Assignment> findByStudentId(Long studentId);
}


