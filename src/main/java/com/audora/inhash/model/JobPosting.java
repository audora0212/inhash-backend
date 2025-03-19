package com.audora.inhash.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;    // 기관명
    private String experience;     // 경력
    private String jobDescription; // 상세직무
    private String location;       // 근무지
    private LocalDate deadline;    // 마감일
    private String dDay;           // 디데이 (예: "D-10")
}
