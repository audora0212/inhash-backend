# INHASH Backend TODO List

## 🚨 배포 전 필수 작업

### 1. 임시 크롤링 코드 삭제 (중요!)
다음 파일들은 개발 환경 테스트 용도로만 작성된 임시 코드입니다.
**프로덕션 배포 전 반드시 삭제해야 합니다.**

#### 삭제해야 할 파일:
- [ ] `/src/main/java/com/inhash/backend/controller/DirectCrawlController.java`
- [ ] `/src/main/java/com/inhash/backend/service/DirectCrawlService.java`
- [ ] `/src/main/java/com/inhash/backend/dto/DirectCrawlRequest.java`
- [ ] `/src/main/java/com/inhash/backend/dto/DirectCrawlResponse.java`

#### 삭제 이유:
- LMS 계정 정보를 서버로 전송받는 것은 **보안상 매우 위험**
- 개인정보보호법 위반 가능성
- 실제 서비스에서는 클라이언트 사이드 크롤링만 사용해야 함
- 서버에 LMS 계정을 저장하지 않는 것이 핵심 보안 원칙

### 2. CORS 설정 수정
- [ ] `WebConfig.java`의 allowed origins를 프로덕션 도메인으로 변경
- [ ] 개발용 localhost 주소 제거
- [ ] `SimpleCorsConfig.java` 삭제 또는 프로덕션 설정으로 변경

### 3. 데이터베이스 설정
- [ ] H2 데이터베이스 의존성 제거
- [ ] MySQL 프로덕션 설정 추가
- [ ] `application.properties`를 `application-prod.properties`로 분리

### 4. 보안 강화
- [ ] Spring Security 추가 고려
- [ ] JWT 토큰 기반 인증 구현
- [ ] API Rate Limiting 추가
- [ ] 입력값 검증 강화

## ✅ 완료된 작업

### 기본 기능
- [x] 회원가입/로그인 API
- [x] 클라이언트 크롤링 데이터 수신 API
- [x] 과제/수업 완료 상태 토글 API
- [x] 데드라인 조회 API
- [x] CORS 설정

### 엔티티
- [x] Student 엔티티
- [x] Assignment 엔티티
- [x] Lecture 엔티티
- [x] UpdateStatus 엔티티

## 📝 향후 개선 사항

### 기능 추가
- [ ] 푸시 알림 (FCM) 구현
- [ ] 과제 통계 API
- [ ] 사용자 설정 저장 API
- [ ] 백업/복원 기능

### 성능 최적화
- [ ] 쿼리 최적화
- [ ] 캐싱 전략 수립
- [ ] 인덱스 추가
- [ ] 페이지네이션 구현

### 모니터링
- [ ] 로깅 시스템 구축
- [ ] 에러 트래킹 (Sentry 등)
- [ ] API 사용량 모니터링
- [ ] 성능 메트릭 수집

## ⚠️ 주의사항

1. **절대 LMS 계정 정보를 서버에 저장하지 마세요**
2. **클라이언트 사이드 크롤링 원칙을 유지하세요**
3. **개인정보는 최소한만 수집하세요**
4. **모든 API에 인증을 적용하세요**

## 📅 배포 체크리스트

배포 전 확인 사항:
- [ ] 모든 임시 코드 삭제 완료
- [ ] 프로덕션 데이터베이스 설정 완료
- [ ] CORS 프로덕션 설정 완료
- [ ] 환경 변수 설정 완료
- [ ] 보안 검토 완료
- [ ] 성능 테스트 완료
- [ ] 백업 계획 수립

---

