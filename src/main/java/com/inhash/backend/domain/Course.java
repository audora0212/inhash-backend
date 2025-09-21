package com.inhash.backend.domain;

import jakarta.persistence.*;

/**
 * LMS에서 제공되는 "과목"(강좌) 정보를 저장합니다.
 * - id는 외부 LMS의 코스 ID를 해시/가공하여 사용합니다.
 * - mainLink는 과목 메인 페이지 URL입니다.
 */
@Entity
@Table(name = "courses")
public class Course {
    @Id
    private String id; // lmsId
    private String name;
    private String mainLink;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMainLink() { return mainLink; }
    public void setMainLink(String mainLink) { this.mainLink = mainLink; }
}


