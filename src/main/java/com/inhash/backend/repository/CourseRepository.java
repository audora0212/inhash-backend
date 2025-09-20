package com.inhash.backend.repository;

import com.inhash.backend.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, String> {}


