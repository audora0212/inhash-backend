package com.inhash.backend.repository;

import com.inhash.backend.domain.Lecture;
import com.inhash.backend.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, String> {
    List<Lecture> findByStudentId(Long studentId);
    List<Lecture> findByStudent(Student student);
    
    @Modifying
    @Transactional
    void deleteByStudent(Student student);
}


