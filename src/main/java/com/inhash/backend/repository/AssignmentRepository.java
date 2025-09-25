package com.inhash.backend.repository;

import com.inhash.backend.domain.Assignment;
import com.inhash.backend.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    List<Assignment> findByStudentId(Long studentId);
    List<Assignment> findByStudent(Student student);
    
    @Modifying
    @Transactional
    void deleteByStudent(Student student);
}


