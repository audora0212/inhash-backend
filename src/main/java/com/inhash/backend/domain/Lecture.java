package com.inhash.backend.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

@Entity
@Table(name = "lectures")
public class Lecture {
    @Id
    private String id; // lmsInstanceId or URL hash
    @ManyToOne(optional = false)
    private Course course;
    @ManyToOne(optional = false)
    @JsonIgnore
    private Student student;
    private String title;
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


