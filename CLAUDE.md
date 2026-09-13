# trip
숙박 공급사 API를 하나의 상품 모델로 통합 검색하는 백엔드

## 스택
Java 25, Spring Boot 4.1 MVC, WebClient, JPA, H2, Lombok, springdoc

## 명령어
`./gradlew build|test|integrationTest|bootRun` (`test`는 단위, `integrationTest`는 `@IntegrationTest`). Mock은 profile `supplier`, port 9090

## 구조
`com.trip.{도메인}`: controller→business→implement→dataaccess, vo

## 외부 API·환경
공급사는 Mock만 호출. `core.hooksPath=.githooks`

## 주의
- 미결정(`.claude/references/decisions.md`)은 묻는다
- 금지어·기획 원문 금지
- 한국어 답변

## 자주 하는 실수
- "권장"만 보고 채택
- 공급사 DTO 외부 노출

## 스킬
/feature /test-write /push /git
