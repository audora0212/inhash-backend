package com.inhash.backend.web.dto;

import java.util.List;

/**
 * 클라이언트에서 크롤링한 데이터를 수신하는 DTO
 * 앱에서 WebView로 LMS에 로그인하여 직접 크롤링한 결과를 전송
 */
public class ClientCrawlDataDto {
    
    private String clientVersion;
    private String clientPlatform; // iOS, Android, Web
    private String crawledAt; // ISO 8601 format
    private List<CourseDto> courses;
    private List<ItemDto> items;
    
    public static class CourseDto {
        private String name;
        private String mainLink;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getMainLink() { return mainLink; }
        public void setMainLink(String mainLink) { this.mainLink = mainLink; }
    }
    
    public static class ItemDto {
        private String type; // assignment, class
        private String courseName;
        private String title;
        private String url;
        private String due; // YYYY-MM-DD HH:MM:SS format
        private Long remainingSeconds;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getDue() { return due; }
        public void setDue(String due) { this.due = due; }
        
        public Long getRemainingSeconds() { return remainingSeconds; }
        public void setRemainingSeconds(Long remainingSeconds) { 
            this.remainingSeconds = remainingSeconds; 
        }
    }
    
    public String getClientVersion() { return clientVersion; }
    public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
    
    public String getClientPlatform() { return clientPlatform; }
    public void setClientPlatform(String clientPlatform) { this.clientPlatform = clientPlatform; }
    
    public String getCrawledAt() { return crawledAt; }
    public void setCrawledAt(String crawledAt) { this.crawledAt = crawledAt; }
    
    public List<CourseDto> getCourses() { return courses; }
    public void setCourses(List<CourseDto> courses) { this.courses = courses; }
    
    public List<ItemDto> getItems() { return items; }
    public void setItems(List<ItemDto> items) { this.items = items; }
}


