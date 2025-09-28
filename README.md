## INHADUE 백엔드 (Spring Boot)

INHADUE 서비스를 위한 Spring Boot 3 기반 REST API입니다. 

### 핵심 아키텍처 원칙
- **클라이언트 사이드 크롤링**: 모든 LMS 데이터 수집은 학생의 기기(iOS/Android 앱)에서 직접 수행됩니다
- **서버는 수동적 수신자**: 서버는 절대 LMS에 직접 접근하지 않으며, 앱에서 보낸 최소한의 메타데이터만 저장합니다
- **프라이버시 우선**: LMS 자격증명(ID/PW)은 서버에 저장되지 않으며, 오직 학생 기기에만 보관됩니다
- **최소 데이터 원칙**: 과제/강의 제목, 마감일 등 필수 정보만 수집하여 서비스를 제공합니다

### 주요 기능
- 학생 계정 관리 (INHADUE 서비스 계정, LMS 계정과 별개)
- 학생 기기에서 크롤링한 과제/강의 메타데이터 수신 및 저장
- 저장된 일정 데이터 조회 API 제공
- 과제/강의 완료 상태 관리

**참고**: 서버 측 LMS 크롤링/계정 저장 코드는 개발팀 내부 테스트 편의를 위한 개발 전용 기능이며, 프로덕션에서는 완전히 비활성화됩니다.

### 기술 스택
- Java 17, Spring Boot 3 (Web, Data JPA, Validation, Actuator)
- MySQL (기본), H2 (런타임 의존성 포함)
- JPA/Hibernate, Jackson (JSR-310)
- Gradle

### 디렉터리 개요
- `controller`: REST 엔드포인트
- `domain`: JPA 엔티티 (`Student`, `Assignment`, `Lecture` 등)
- `repository`: Spring Data JPA 리포지토리
- `service`: 크롤링/가져오기, 동기화 잡, LMS 계정 관리 등
- `config`: CORS 및 비동기 실행 설정

---

### 시작하기

1) 사전 준비
- Java 17+
- Gradle(래퍼 포함)
- 로컬 MySQL 실행(또는 데이터소스 설정 오버라이드)

2) 애플리케이션 설정
- 기본 프로필은 `dev`이며 기본 서버 포트는 `8080`입니다.
- 기본 데이터소스(`src/main/resources/application.properties`):
  - URL: `jdbc:mysql://localhost:3306/inhash?...`
  - Username: `root`
  - Password: `12341234`

환경변수 또는 JVM 옵션으로 오버라이드할 수 있습니다.
- `-Dspring.datasource.url=...`
- `-Dspring.datasource.username=...`
- `-Dspring.datasource.password=...`
- `-Dserver.port=...`
- `-Dspring.profiles.active=dev` (또는 `prod`)

3) 빌드 및 실행

```bash
./gradlew clean build
./gradlew bootRun
# 또는 빌드 결과 실행
java -jar build/libs/inhash-backend-0.0.1-SNAPSHOT.jar
```

Actuator 헬스: `/actuator/health`
애플리케이션 헬스: `/api/health`

---

### 구성 및 프로필

- 기본값: `spring.profiles.active=dev`
- 주요 속성:
  - JPA: `spring.jpa.hibernate.ddl-auto=update`, MySQL dialect, `spring.jpa.open-in-view=false`
  - 스케줄러: `inhash.scheduler.enabled`, `inhash.scheduler.cron`, `inhash.scheduler.crawl.enabled`
  - 개발용 크롤러: `inhash.devtools.crawler.enabled`, `inhash.devtools.crawler.python.*`

### CORS

허용 오리진(내부 로컬 테스트용):
- `http://localhost:3000`
- `http://localhost:3001`
- `http://127.0.0.1:3000`
- `http://127.0.0.1:3001`

허용 메서드: GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD
자격증명 허용. 자세한 내용은 `config/WebConfig.java`, `config/SimpleCorsConfig.java` 참고.

---

### API 명세

Base URL: `/api`

#### Health
- GET `/api/health`
  - 200: `{ "status": "OK", "ts": "<ISO-8601>" }`

#### Schedules (관리/개발 유틸리티)
- GET `/api/schedules?studentId=<optional>`
  - 200: `{ assignments: [...], lectures: [...], counts: { assignments, lectures } }`

#### Sync Logs (관리/개발 유틸리티)
- GET `/api/sync-logs`
  - 200: `{ count: number, logs: [...] }`

#### Jobs (관리/개발 유틸리티)
- GET `/api/jobs/{jobId}`
  - 200: 잡 객체
  - 404: 없음

---

### 인증(Auth)

Prefix: `/api/auth`

- POST `/api/auth/signup`
  - 요청(JSON):
    ```json
    { "email": "user@example.com", "password": "secret", "name": "홍길동" }
    ```
  - 200:
    ```json
    { "success": true, "studentId": 1, "token": "<uuid>", "email": "user@example.com", "name": "홍길동" }
    ```
  - 400: `{ "success": false, "error": "..." }`

- POST `/api/auth/login`
  - 요청:
    ```json
    { "email": "user@example.com", "password": "secret" }
    ```
  - 200:
    ```json
    { "success": true, "studentId": 1, "token": "<uuid>", "email": "user@example.com", "name": "홍길동", "lmsLinked": true }
    ```
  - 400: `{ "success": false, "error": "..." }`

- DELETE `/api/auth/delete`
  - 요청:
    ```json
    { "studentId": 1 }
    ```
  - 200: `{ "success": true, "message": "Account and all related data deleted successfully" }`
  - 404/500: `{ success: false, error: "..." }`

- DELETE `/api/auth/delete/{studentId}`
  - 위와 동일하나 PathVariable 사용

---

### LMS 계정 및 동기화 (테스트 전용)

- POST `/api/lms/sync` (내부 테스트용)
  - 즉시 크롤링/가져오기를 트리거합니다.
  - 200: `{ "status": "triggered" }`

Prefix: `/api/lms/accounts`

- POST `/api/lms/accounts` (내부 테스트용)
  - 요청:
    ```json
    { "studentId": 1, "username": "lms_id", "password": "lms_pw" }
    ```
  - 202 Accepted:
    ```json
    { "id": 10, "jobId": "<job-id>", "status": "queued" }
    ```

- PUT `/api/lms/accounts` (내부 테스트용)
  - 요청: POST와 동일
  - 200: `{ "id": 10, "status": "updated" }`

---

### 마감(Deadlines)

Prefix: `/api/deadlines`

- GET `/api/deadlines/{studentId}`
  - 아직 마감되지 않은 과제/강의 반환
  - 200 예시:
    ```json
    {
      "success": true,
      "assignments": [
        { "id": "a1", "title": "과제1", "courseName": "자료구조", "url": "https://...", "completed": false, "type": "assignment", "dueAt": "2025-09-28T15:00:00", "dueDate": "2025-09-28T15:00:00", "remainingDays": 2 }
      ],
      "lectures": [
        { "id": "l1", "title": "1주차 강의", "courseName": "자료구조", "url": "https://...", "completed": false, "type": "lecture", "dueAt": "2025-09-29T23:59:59", "lectureDate": "2025-09-29T23:59:59", "remainingDays": 1 }
      ]
    }
    ```

- GET `/api/deadlines/{studentId}/all`
  - 과거 포함 전체 과제/강의 반환

- GET `/api/deadlines/{studentId}/today`
  - 오늘 마감 항목 반환

---

### 완료 토글(Completion)

Prefix: `/api/completion`

- POST `/api/completion/assignment/{id}/toggle?studentId=1`
  - 200: `{ "success": true, "completed": true, "message": "Assignment marked as completed" }`

- POST `/api/completion/lecture/{id}/toggle?studentId=1`
  - 200: `{ "success": true, "completed": false, "message": "Lecture marked as incomplete" }`

---

### 클라이언트 크롤 데이터 수신

**중요: 데이터 수집 방식**
- **서버는 절대 학생의 LMS에 직접 접근하지 않습니다**
- **모든 크롤링은 학생의 기기(iOS/Android 앱)에서 직접 수행됩니다**
- **앱은 학생이 직접 LMS에 로그인하여 WebView 내에서 크롤링을 수행합니다**
- **서버는 오직 크롤링된 결과 중 최소한의 메타데이터만 수신합니다**

#### 데이터 수집 프로세스:
1. 학생이 앱에서 직접 LMS 로그인 (WebView 사용)
2. 앱이 학생 기기에서 JavaScript를 통해 과제/강의 정보 크롤링
3. 크롤링된 데이터 중 필수 정보만 추출 (제목, 마감일, URL 등)
4. 최소화된 데이터를 서버로 전송
5. 서버는 받은 데이터를 단순 저장만 수행

**서버가 저장하는 정보 (최소한의 메타데이터):**
- 과제/강의 제목
- 과목명
- 마감일시
- LMS 내 URL (참조용)
- 완료 여부

**서버가 저장하지 않는 정보:**
- LMS 로그인 자격증명 (ID/PW)
- 과제 내용 상세
- 개인 성적 정보
- 기타 민감한 학사 정보

Prefix: `/api/crawl`

- POST `/api/crawl/submit/{studentId}`
  - 요청 바디(ClientCrawlDataDto) - 앱에서 크롤링 후 전송하는 데이터:
    ```json
    {
      "clientVersion": "1.0.0",
      "clientPlatform": "iOS",
      "crawledAt": "2025-09-28T12:00:00",
      "courses": [ { "name": "자료구조", "mainLink": "https://..." } ],
      "items": [
        { "type": "assignment", "courseName": "자료구조", "title": "과제1", "url": "https://...", "due": "2025-10-01 23:59:59", "remainingSeconds": 86400 }
      ]
    }
    ```
  - 200: `{ "success": true, "imported": 12, "message": "데이터가 성공적으로 업데이트되었습니다." }`

- DELETE `/api/crawl/delete/{studentId}`
  - 200: `{ "success": true, "message": "학생 데이터가 삭제되었습니다." }`

- GET `/api/crawl/status/{studentId}`
  - 200: `{ "studentId": 1, "lastUpdated": null, "needsUpdate": true }`

---

### 개발/테스트 전용 엔드포인트

**경고: 이 섹션의 모든 기능은 개발 편의를 위한 것으로, 프로덕션에서는 절대 사용되지 않습니다.**

다음 엔드포인트는 `dev`/`test`에서만 활성화되며, 프로덕션에서는 완전히 비활성화하거나 제거됩니다.
이는 개발팀이 앱 없이도 백엔드 기능을 테스트하기 위한 임시 도구입니다.

Prefix: `/api/devtools/crawler`

- POST `/api/devtools/crawler/execute`
  - 요청:
    ```json
    { "studentId": 1, "username": "lms_id", "password": "lms_pw" }
    ```
  - 200: `{ "success": true, "result": { ... }, "warning": "This is a development feature..." }`

- GET `/api/devtools/crawler/status`
  - 200: `{ "credentialCount": 0, "warning": "Development mode only" }`

- DELETE `/api/devtools/crawler/clear`
  - 200: `{ "success": true, "message": "All temporary credentials cleared" }`

추가(개발용) `/api/crawl` 엔드포인트:
- POST `/api/crawl/direct` — 자격증명을 이용한 서버 크롤링(개발용)
- POST `/api/crawl/test` — 내부 크롤러 테스트

**보안/법적 주의사항**: 
- 서버가 제3자(학생) LMS 자격증명을 보관·사용하거나 서버에서 직접 크롤링을 수행하는 것은 심각한 법적 리스크와 계정 유출 위험이 있습니다
- 위 개발용 기능은 오직 개발팀 내부 테스트 목적으로만 사용되며, 실제 학생 계정으로는 절대 사용하지 않습니다
- **프로덕션 환경에서는 반드시 비활성화되며, 모든 데이터 수집은 학생 기기에서만 수행됩니다**
- INHADUE 서비스는 학생이 자신의 기기에서 직접 LMS에 로그인하고, 자신의 데이터를 자발적으로 수집하는 방식으로만 운영됩니다

#### 프로덕션 비활성화 가이드
- 설정에서 개발용 크롤러 비활성화: `inhash.devtools.crawler.enabled=false`
- 배포 전 다음 컨트롤러/라우트를 제거하거나 프로필 가드(@Profile 등)로 보호:
  - `DevCrawlerController` (`/api/devtools/crawler/*`)
  - `DirectCrawlController` (`/api/crawl/direct`)
  - `CrawlDebugController` (`/api/crawl/test`)
  - `LmsAccountController` (`/api/lms/accounts` 전반)
  - `LmsController`의 `/api/lms/sync`
- 필요 시 API 게이트웨이/리버스 프록시에서 상기 경로 차단

---

### 엔티티 요약

- Assignment: `{ id: string, courseName: string, title: string, url: string, dueAt: Instant, completed: boolean }`
- Lecture: `{ id: string, courseName: string, title: string, url: string, dueAt: Instant, completed: boolean }`
- Student: `{ id: number, email: string, name: string, passwordHash: string }`

---

### 참고사항
- 현재 비밀번호는 데모 목적으로 단순 해시로 저장됩니다. 실제 운영 환경에서는 AWS KMS를 주입받아 암호화할 예정입니다.


