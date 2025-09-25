package com.inhash.backend.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * 학생이 수강해야 하는 "강의(차시)" 항목입니다.
 * - iClass/VOD 등 플랫폼별 형식을 통합해 저장합니다.
 * - dueAt은 수강 가능 기간의 종료 시각을 의미합니다.
 */
@Entity
@Table(name = "lectures")
public class Lecture {
    @Id
    @Column(length = 255)
    /** 강의(차시) 식별자: 외부 인스턴스 ID 또는 (제목|URL|코스|학생) 해시 */
    private String id; // lmsInstanceId or URL hash
    @ManyToOne(optional = false)
    private Course course;
    @ManyToOne(optional = false)
    @JsonIgnore
    private Student student;
    @Column(length = 500)
    private String title;
    @Column(length = 1000)
    private String url;
    private Instant dueAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
}


