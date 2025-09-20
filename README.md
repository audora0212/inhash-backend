# INHASH Backend (Spring Boot)

## Quick Start
- Java 17 필요
- 로컬 내 파이썬 크롤러 경로는 `src/main/resources/application.yml`에 설정되어 있습니다.

### 실행
1) IDE에서 `InhashBackendApplication` 실행 또는
2) Maven 설치가 없다면 IDE 빌드 사용. (mvn 미설치 환경)

서버가 뜨면:
- Health: GET http://localhost:8080/api/health
- Schedules: GET http://localhost:8080/api/schedules

### 크롤링 연동
- 애플리케이션 시작 후 스케줄러가 cron에 맞춰 실행됩니다.
- 수동 테스트: `CrawlService#runCrawlAndImport()` 호출하거나 cron을 임시 변경해 주기 실행으로 테스트.

### 설정(application.yml)
- inhash.crawl.python.workingDir: 파이썬 `final.py`가 있는 디렉토리
- inhash.crawl.python.executable: 파이썬 실행 파일 (예: python)
- inhash.crawl.python.script: 실행할 스크립트 (final.py)
- inhash.scheduler.cron: 스케줄 크론식

## 주의
- 현재 H2(메모리) DB 사용, 재시작 시 데이터 초기화됨.
- 운영 시 MySQL/PostgreSQL 등으로 교체 권장.
