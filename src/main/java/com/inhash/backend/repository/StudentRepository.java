package com.inhash.backend.repository;

import com.inhash.backend.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {}


