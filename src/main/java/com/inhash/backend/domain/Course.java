package com.inhash.backend.domain;

import jakarta.persistence.*;

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


