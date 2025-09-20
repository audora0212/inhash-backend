package com.inhash.backend.controller;

import com.inhash.backend.domain.Assignment;
import com.inhash.backend.domain.Lecture;
import com.inhash.backend.repository.AssignmentRepository;
import com.inhash.backend.repository.CourseRepository;
import com.inhash.backend.repository.LectureRepository;
import com.inhash.backend.repository.SyncLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiControllers {

    private final AssignmentRepository assignmentRepository;
    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final SyncLogRepository syncLogRepository;

    public ApiControllers(AssignmentRepository assignmentRepository, LectureRepository lectureRepository, CourseRepository courseRepository, SyncLogRepository syncLogRepository) {
        this.assignmentRepository = assignmentRepository;
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
        this.syncLogRepository = syncLogRepository;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "OK");
        m.put("ts", Instant.now().toString());
        return m;
    }

    @GetMapping("/schedules")
    public Map<String, Object> schedules(@RequestParam(name = "studentId", required = false) Long studentId) {
        List<Assignment> assignments = (studentId == null) ? assignmentRepository.findAll() : assignmentRepository.findByStudentId(studentId);
        List<Lecture> lectures = (studentId == null) ? lectureRepository.findAll() : lectureRepository.findByStudentId(studentId);
        Map<String, Object> m = new HashMap<>();
        m.put("assignments", assignments);
        m.put("lectures", lectures);
        m.put("counts", Map.of(
                "assignments", assignments.size(),
                "lectures", lectures.size()
        ));
        return m;
    }

    @GetMapping("/sync-logs")
    public Map<String, Object> syncLogs() {
        var logs = syncLogRepository.findAll();
        return Map.of(
                "count", logs.size(),
                "logs", logs
        );
    }
}


