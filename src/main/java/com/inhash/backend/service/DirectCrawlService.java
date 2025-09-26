package com.inhash.backend.service;

import com.inhash.backend.dto.DirectCrawlResponse;
import com.inhash.backend.dto.DirectCrawlResponse.*;
import com.inhash.backend.domain.Assignment;
import com.inhash.backend.domain.Lecture;
import com.inhash.backend.domain.Student;
import com.inhash.backend.repository.AssignmentRepository;
import com.inhash.backend.repository.LectureRepository;
import com.inhash.backend.repository.StudentRepository;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * [임시 코드 - 배포 시 삭제 필요]
 * 개발자 전용 직접 크롤링 서비스
 * 실제 LMS에 접속하여 데이터를 크롤링
 * 
 * WARNING: 이 서비스는 개발 환경 테스트 용도로만 사용됩니다.
 * 프로덕션 배포 전 반드시 삭제해야 합니다.
 * 
 * 현재는 실제 크롤링 대신 테스트 데이터만 생성합니다.
 * 실제 LMS 크롤링 코드는 주석 처리되어 있으며, 
 * 프로덕션에서는 클라이언트 사이드 크롤링만 사용해야 합니다.
 */
@Service
@Transactional
public class DirectCrawlService {

    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private LectureRepository lectureRepository;

    private static final String LMS_LOGIN_URL = "https://learn.inha.ac.kr/login/index.php";
    private static final String LMS_DASHBOARD_URL = "https://learn.inha.ac.kr/my";

    public DirectCrawlResponse crawlLMS(Long studentId, String lmsUsername, String lmsPassword) {
        try {
            // ============================================
            // [임시 코드] 개발 환경에서는 실제 크롤링 대신 테스트 데이터 반환
            // 배포 시 이 메소드를 포함한 전체 클래스 삭제 필요
            // ============================================
            return createTestData(studentId);
            
            // 실제 크롤링 코드 (주석 처리)
            /*
            // 1. LMS 로그인
            Map<String, String> cookies = loginToLMS(lmsUsername, lmsPassword);
            
            // 2. 대시보드 접속
            Document dashboard = Jsoup.connect(LMS_DASHBOARD_URL)
                .cookies(cookies)
                .get();
            
            // 3. 과제 및 수업 정보 추출
            List<AssignmentDto> assignments = extractAssignments(dashboard);
            List<LectureDto> lectures = extractLectures(dashboard);
            List<CourseDto> courses = extractCourses(dashboard);
            
            // 4. DB에 저장
            saveToDatabase(studentId, assignments, lectures);
            
            return DirectCrawlResponse.builder()
                .success(true)
                .message("크롤링 성공")
                .assignmentCount(assignments.size())
                .lectureCount(lectures.size())
                .courses(courses)
                .assignments(assignments)
                .lectures(lectures)
                .build();
            */
            
        } catch (Exception e) {
            e.printStackTrace();
            return DirectCrawlResponse.builder()
                .success(false)
                .message("크롤링 실패: " + e.getMessage())
                .assignmentCount(0)
                .lectureCount(0)
                .build();
        }
    }
    
    /**
     * 테스트 데이터 생성 (개발용)
     */
    private DirectCrawlResponse createTestData(Long studentId) {
        // 학생 확인
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return DirectCrawlResponse.builder()
                .success(false)
                .message("학생 정보를 찾을 수 없습니다.")
                .build();
        }
        
        // 테스트 과목 데이터
        List<CourseDto> courses = Arrays.asList(
            CourseDto.builder().id("1").name("웹프로그래밍").mainLink("https://learn.inha.ac.kr/course/view.php?id=1").build(),
            CourseDto.builder().id("2").name("데이터베이스").mainLink("https://learn.inha.ac.kr/course/view.php?id=2").build(),
            CourseDto.builder().id("3").name("알고리즘").mainLink("https://learn.inha.ac.kr/course/view.php?id=3").build(),
            CourseDto.builder().id("4").name("운영체제").mainLink("https://learn.inha.ac.kr/course/view.php?id=4").build(),
            CourseDto.builder().id("5").name("소프트웨어공학").mainLink("https://learn.inha.ac.kr/course/view.php?id=5").build()
        );
        
        // 현재 시간 기준으로 날짜 생성
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        // 테스트 과제 데이터
        List<AssignmentDto> assignments = Arrays.asList(
            AssignmentDto.builder()
                .title("중간 프로젝트 제출")
                .courseName("웹프로그래밍")
                .courseId("1")
                .due(now.plusDays(5).format(formatter))
                .url("#")
                .build(),
            AssignmentDto.builder()
                .title("SQL 쿼리 최적화 과제")
                .courseName("데이터베이스")
                .courseId("2")
                .due(now.plusDays(3).format(formatter))
                .url("#")
                .build(),
            AssignmentDto.builder()
                .title("정렬 알고리즘 구현")
                .courseName("알고리즘")
                .courseId("3")
                .due(now.plusDays(7).format(formatter))
                .url("#")
                .build(),
            AssignmentDto.builder()
                .title("프로세스 스케줄링 분석")
                .courseName("운영체제")
                .courseId("4")
                .due(now.plusDays(10).format(formatter))
                .url("#")
                .build(),
            AssignmentDto.builder()
                .title("요구사항 명세서 작성")
                .courseName("소프트웨어공학")
                .courseId("5")
                .due(now.plusDays(2).format(formatter))
                .url("#")
                .build()
        );
        
        // 테스트 수업 데이터
        List<LectureDto> lectures = Arrays.asList(
            LectureDto.builder()
                .title("REST API 설계 원칙")
                .courseName("웹프로그래밍")
                .courseId("1")
                .due(now.plusDays(1).withHour(14).withMinute(0).format(formatter))
                .url("#")
                .build(),
            LectureDto.builder()
                .title("트랜잭션과 동시성 제어")
                .courseName("데이터베이스")
                .courseId("2")
                .due(now.plusDays(2).withHour(10).withMinute(0).format(formatter))
                .url("#")
                .build(),
            LectureDto.builder()
                .title("그래프 알고리즘")
                .courseName("알고리즘")
                .courseId("3")
                .due(now.plusDays(3).withHour(15).withMinute(0).format(formatter))
                .url("#")
                .build()
        );
        
        // DB에 저장
        saveTestDataToDatabase(student, assignments, lectures);
        
        return DirectCrawlResponse.builder()
            .success(true)
            .message("테스트 데이터 로드 성공")
            .assignmentCount(assignments.size())
            .lectureCount(lectures.size())
            .courses(courses)
            .assignments(assignments)
            .lectures(lectures)
            .build();
    }
    
    /**
     * 테스트 데이터를 DB에 저장
     */
    private void saveTestDataToDatabase(Student student, List<AssignmentDto> assignments, List<LectureDto> lectures) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        // 기존 데이터 삭제
        assignmentRepository.deleteByStudent(student);
        lectureRepository.deleteByStudent(student);
        
        // 과제 저장
        for (AssignmentDto dto : assignments) {
            String id = generateId(dto.getTitle(), dto.getCourseName(), student.getId(), dto.getDue());
            
            Assignment assignment = new Assignment();
            assignment.setId(id);
            assignment.setStudent(student);
            assignment.setCourseName(dto.getCourseName());
            assignment.setTitle(dto.getTitle());
            assignment.setDueAt(parseDateTime(dto.getDue()));
            assignment.setCompleted(false);
            
            assignmentRepository.save(assignment);
        }
        
        // 수업 저장
        for (LectureDto dto : lectures) {
            String id = generateId(dto.getTitle(), dto.getCourseName(), student.getId(), dto.getDue());
            
            Lecture lecture = new Lecture();
            lecture.setId(id);
            lecture.setStudent(student);
            lecture.setCourseName(dto.getCourseName());
            lecture.setTitle(dto.getTitle());
            lecture.setDueAt(parseDateTime(dto.getDue()));
            lecture.setCompleted(false);
            
            lectureRepository.save(lecture);
        }
    }
    
    private String generateId(String title, String course, Long studentId, String due) {
        String combined = title + "|" + course + "|" + studentId + "|" + due;
        return Integer.toHexString(combined.hashCode());
    }
    
    private Instant parseDateTime(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime ldt = LocalDateTime.parse(dateStr, formatter);
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return Instant.now().plusSeconds(86400); // 1일 후
        }
    }
}
