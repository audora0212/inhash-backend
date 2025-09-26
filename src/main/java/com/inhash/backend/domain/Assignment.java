package com.inhash.backend.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * 학생에게 부여된 "과제"를 표현합니다.
 * - 같은 제목/링크라도 학생·과목이 다르면 별도 항목으로 관리됩니다.
 * - 마감 시각(dueAt)은 KST 기준으로 파싱되어 저장됩니다.
 */
@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    @Column(length = 255)
    /**
     * 과제 식별자.
     * 외부 LMS의 인스턴스 ID나 (제목|URL|코스|학생) 조합의 해시를 사용합니다.
     */
    private String id; // lmsInstanceId or URL hash
    @Column(length = 200)
    private String courseName;  // 과목명 직접 저장
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
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { 
        this.courseName = courseName;
    }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
}


