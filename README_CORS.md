# CORS 설정 가이드

## 문제 해결
웹 애플리케이션에서 백엔드 API 호출 시 CORS 오류가 발생하는 경우 이 가이드를 따라주세요.

## CORS 설정 파일

### 1. Spring Security를 사용하는 경우
- `SecurityConfig.java` 파일 사용
- Spring Security와 함께 CORS 설정이 적용됩니다

### 2. Spring Security를 사용하지 않는 경우
다음 중 하나를 선택하세요:
- `WebConfig.java` - Spring MVC 기본 CORS 설정
- `SimpleCorsConfig.java` - 필터 기반 간단한 CORS 설정

## 백엔드 재시작 방법

### Gradle 사용
```bash
cd /Users/choeyeongchan/Desktop/INHADUE/inhash-backend

# 기존 프로세스 종료
./gradlew --stop

# 클린 빌드 및 실행
./gradlew clean build
./gradlew bootRun
```

### IDE 사용 (IntelliJ IDEA)
1. 기존 실행 중인 애플리케이션 중지 (빨간 정지 버튼)
2. `Build` → `Rebuild Project`
3. `InhashBackendApplication.java` 실행

## 설정 확인

백엔드가 재시작되면 다음을 확인하세요:

1. 콘솔에서 CORS 관련 로그 확인
2. 브라우저 개발자 도구에서 네트워크 탭 확인
3. Preflight (OPTIONS) 요청이 200 OK 응답을 받는지 확인

## 허용된 Origin

현재 설정된 허용 origin:
- http://localhost:3000
- http://localhost:3001
- http://127.0.0.1:3000
- http://127.0.0.1:3001

## 문제가 지속되는 경우

1. **브라우저 캐시 삭제**
   - Chrome: Cmd+Shift+Delete → 캐시된 이미지 및 파일 삭제

2. **포트 확인**
   - 백엔드: 8080 포트
   - 프론트엔드: 3000 포트

3. **application.properties 확인**
   ```properties
   server.port=8080
   ```

4. **프론트엔드 환경변수 확인**
   - `.env.local` 파일 생성 (필요시)
   ```
   NEXT_PUBLIC_API_URL=http://localhost:8080
   ```

## 테스트 방법

```bash
# CORS 테스트 (터미널에서)
curl -X OPTIONS http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

성공 시 다음과 같은 헤더를 확인할 수 있습니다:
- Access-Control-Allow-Origin: http://localhost:3000
- Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
- Access-Control-Allow-Headers: *
- Access-Control-Allow-Credentials: true

