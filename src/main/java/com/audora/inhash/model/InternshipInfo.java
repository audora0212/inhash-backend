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
public class InternshipInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String institutionName;       // 기관(법인)명
    private Boolean governmentFunded;       // 국고사업여부
    private String department;              // 부서
    private String workDepartmentName;      // 근무 부서명
    private String jobTitle;                // 직무명
    private String location;                // 소재지
    private String internshipPeriod;        // 실습기간 (예: "2025-06-01 ~ 2025-08-31")
    private LocalDate applicationDeadline;  // 접수마감일
    private String recruitmentInfo;         // 모집정보
    private String supportFee;              // 실습지원비
    private String applicationLink;         // 지원(신청 링크 등)
}
