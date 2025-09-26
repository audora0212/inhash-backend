package com.inhash.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DirectCrawlResponse {
    private boolean success;
    private String message;
    private int assignmentCount;
    private int lectureCount;
    private List<CourseDto> courses;
    private List<AssignmentDto> assignments;
    private List<LectureDto> lectures;
    
    @Data
    @Builder
    public static class CourseDto {
        private String id;
        private String name;
        private String mainLink;
    }
    
    @Data
    @Builder
    public static class AssignmentDto {
        private String title;
        private String courseName;
        private String courseId;
        private String due;
        private String url;
    }
    
    @Data
    @Builder
    public static class LectureDto {
        private String title;
        private String courseName;
        private String courseId;
        private String due;
        private String url;
    }
}

