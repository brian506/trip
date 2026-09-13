# 결정 현황

사용자가 확정한 것만 구현에 반영한다. 미결정 항목은 선택지와 트레이드오프를 정리해 묻고, 결정되면 이 파일을 갱신한다.
기획 문서의 "권장" 항목이라는 이유만으로 채택하지 않는다. 필수 요구사항만 전제로 둔다.

## 확정

| 항목 | 결정 |
|------|------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1, Spring MVC |
| Build | Gradle Groovy DSL, 단일 모듈 |
| 외부 호출 | Spring WebClient |
| 요청 스레드 | 가상 스레드 (`spring.threads.virtual.enabled`). MVC 위에서 WebClient 병렬 호출 후 서비스 경계에서 `block()` 한 번 |
| DB | H2 (실행·테스트 모두), Spring Data JPA |
| 계층 | `controller → business → implement → dataaccess` + `vo` |
| Implement | 저장소 단위 CRUD는 Manager 하나, 외부 호출은 Client |
| 도메인 검증 | VO 생성자에서 `AppException` |
| 응답·에러 | `ApiResponse`, `AppException`, `ErrorType`, `ErrorCode`, `ApiControllerAdvice` |
| 라이브러리 | Lombok (`lombok.config` 제약), springdoc-openapi |
| 공급사 Mock 서버 | `supplier` 패키지, `@Profile("supplier")`, 9090 포트 별도 프로세스 |
| 기록 | 저장소가 원본. 근거·과정은 `JOURNAL.md`, 이슈에는 요약과 링크 |
| 커밋 | `type[#이슈번호]: 설명`, 이슈는 사용자가 닫는다 |
| 테스트 작성 | `@DisplayName` 자연어가 명세. 정상·경계·예외 상황을 제시하고 사용자가 고른 것만 구현. 구현 로직을 보고 케이스·기댓값을 만들지 않음 |
| 푸시 전 검증 | 구현 `review`, 테스트 `test-review` 리뷰 게이트 + pre-push `clean build` |

## 미결정

- H2 모드(인메모리·파일)와 스키마 관리 방식
- 도메인 패키지 구성과 이름 (`supplier`는 Mock 서버가 사용 중)
- 공급사 요청·응답 DTO의 위치
- 내부 식별자 형식
- 타임아웃·재시도·서킷 브레이커 구현 방식
- 테스트 종류(단위·통합) 구분과 파일 위치 규칙
- 커밋 단위 기준
- 브랜치 전략
- `docs/` 구성
- 매핑 동기화 시점
