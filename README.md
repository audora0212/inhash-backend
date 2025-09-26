## INHASH 백엔드
인하대학교 학생들의 과제와 수업 기한을 자동 수집·저장하고, 마감 전에 FCM 알림을 보내주는 서비스의 백엔드입니다.

> ⚠️ **중요 공지**: 배포 전 반드시 [TODO.md](./TODO.md) 파일을 확인하여 개발용 임시 코드를 삭제하세요!
> DirectCrawlController와 DirectCrawlService는 개발 환경 전용입니다.

### 핵심 목표
- LMS(learn.inha.ac.kr)에서 과제/수업 마감 정보를 수집
- 사용자 가입 시 LMS 계정 연동과 동시에 백그라운드 동기화 수행
- 수집된 일정을 저장·조회하고, FCM으로 적시에 알림 발송

## 아키텍처 개요
- 애플리케이션: Spring Boot 3 (Web, Data JPA, Validation, Actuator)
- 데이터 계층: JPA 엔티티(`Assignment`, `Lecture`, `Course`, `Student`, `LMSAccount`, `SyncJob`, `SyncLog`, `FcmToken`)
- 크롤링: Jsoup 기반 내부 크롤러 또는 외부 파이썬 스크립트(`final.py`) 실행 결과를 파싱
- 비동기/스케줄링: `@EnableAsync` + `ThreadPoolTaskExecutor`, `@EnableScheduling` + `@Scheduled`

### 현재 동기화 플로우(로컬/기본)
1. 회원가입 API가 `Student`/`LMSAccount` 생성, `SyncJob(status=queued)` 등록 후 즉시 응답(202)
2. 백그라운드 작업이 크롤러 실행 → JSON 파싱 → DB 멱등 Upsert(고유키: `Assignment.id`)
3. `SyncJob` 상태/개수 업데이트, 클라이언트는 `jobId`로 진행상태 조회

### 권장 운영 플로우(서버리스)
- 가입/정기 동기화 요청을 SQS에 게시 → Lambda가 처리(크롤+적재)
- 데이터가 커질 경우 S3에 결과 저장, 큐에는 S3 키만 담아 256KB 한도 회피
- 장점: 서버 부하 제거, 버퍼링/재시도/DLQ 표준화, 손쉬운 확장

## 데이터/성능 가정(실측 포함)
- 항목 스키마: `type, course_name, title, url, due`
- 항목당 크기(평균): 300–800B
- 1인 한 달 15–25개 ⇒ 5–20KB 수준
- 실측 예(외부 파이썬 크롤러): 56건, 약 5.2초(tookMs=5241)
- 10명 동시 가입 시(동시성 10): 전체 완료 p50 6–15초, p95 20–40초(외부 LMS 지연에 좌우)

## UX 정책(초기 경험 최적화)
- 회원가입+LMS 연동 성공 즉시 메인으로 이동(로딩 화면 최소)
- 메인에서 “동기화 중” 배너/스켈레톤 표시 → 완료 시 자동 갱신
- 진행상태 확인: `GET /api/jobs/{jobId}` 폴링 또는 일정 데이터 존재 여부 체크

## 고민점 및 의사결정 기록
- SQS만으로 충분한가? ⇒ 현재 데이터 크기/건수 기준 충분. 향후 증가 시 S3(아카이빙/재처리, 256KB 초과 회피) 고려
- Lambda 콜드스타트/DB 연결 폭증 ⇒ SnapStart 또는 경량 런타임, RDS Proxy, 동시성 제한/배치 처리
- LMS 레이트리밋/변경 대응 ⇒ 지수백오프 재시도, DLQ 알람, 파서 유연성 유지, 크롤 주기 분산(DelaySeconds 랜덤)
- 멱등성 ⇒ `Assignment.id = MD5(title|url|course|student)` 유지, DB 유니크 제약으로 중복 방지
- 보안 ⇒ LMS 자격증명은 메시지 본문에 실지 말고 Secrets Manager 등으로 관리(식별자만 주고받기)
- 합법성/약관 ⇒ 대상 LMS의 서비스 약관/정책 준수 범위 확인

## 주요 엔드포인트(예시)
- GET `/api/health` 헬스 체크
- POST `/api/auth/register` 회원가입+LMS 연동(202 Accepted, `{jobId}` 반환)
- GET `/api/jobs/{jobId}` 동기화 진행상태 조회
- GET `/api/schedules` 일정 조회(필요 시 학생 컨텍스트 기반)
- POST `/api/crawl/test` 내부 디버그 크롤(개발용)

## 설정 가이드(application.properties)
- `inhash.crawl.python.workingDir` : 파이썬 `final.py` 위치
- `inhash.crawl.python.executable` : 파이썬 실행 파일 경로(예: python)
- `inhash.crawl.python.script` : 실행할 스크립트 파일명(예: final.py)
- `inhash.scheduler.cron` : 스케줄 크론식(정기 동기화)

## 로컬 실행
- Java 17 필요
- IDE에서 `InhashBackendApplication` 실행 또는
- Gradle(Windows PowerShell): `cd inhash-backend; .\gradlew.bat bootRun`

서버 기동 후:
- Health: `GET http://localhost:8080/api/health`
- Schedules: `GET http://localhost:8080/api/schedules`

## 운영 체크리스트
- SQS: 가시성 타임아웃(p95×3), 재시도/ DLQ, FIFO가 필요하면 `messageGroupId=studentId`
- Lambda: 동시성/시간 제한, 콜드스타트 전략, RDS Proxy 연결
- 모니터링: 처리 지연, DLQ 알람, 동기화 성공/실패율, `tookMs` 히스토그램
- 보관/감사 필요 시: S3에 원본 JSON 저장, 수동 재처리 루틴 준비
