# trip
숙박 공급사 API를 하나의 상품 모델로 통합 검색하는 백엔드

## 스택
Java 25, Spring Boot 4.1 MVC, WebClient, JPA, H2, Lombok, springdoc

## 명령어
`./gradlew build|test|integrationTest|bootRun` (`test`는 단위, `integrationTest`는 `@IntegrationTest`). Mock은 `./gradlew :mock-supplier:bootRun`, port 9090

## 구조
`com.trip.{도메인}`: controller→business→implement→dataaccess, vo. 공급사 연동은 `com.trip.supplier`(도메인→supplier 한 방향), Mock은 별도 Gradle 모듈 `mock-supplier`

## 외부 API·환경
공급사는 Mock만 호출. `core.hooksPath=.githooks`

## 주의
- 미결정(`.claude/references/decisions.md`)은 묻는다
- 금지어·기획 원문 금지
- 로그는 `[카테고리 : 상세내용]: key=value | key=value` 형식으로 통일. 레벨은 정상 `info`, 예상된 실패 `warn`, 시스템 오류 `error`
- 한국어 답변

## 네이밍
조회 동사는 데이터 출처로 구분한다. `find` 내 DB(repository·implement), `fetch` 공급사 호출, `search`·`sync` 등 도메인 동사는 business. 없으면 예외를 던지는 단건 조회에만 `get`을 쓴다(빈 결과 가능하면 `find`).

리포지토리 메서드는 조건이 2개를 넘으면 파생 쿼리 대신 `@Query` JPQL로 쓰고 이름은 짧게 짓는다 — 조건은 이름이 아니라 JPQL 본문이 드러낸다. 동적 조건이 필요해지기 전까지 QueryDSL은 도입하지 않는다.

ID 묶음 파라미터는 `Set`으로 받아 중복을 타입으로 차단한다(`Collection`·`List` 아님).

VO 이름은 파이프라인 단계를 드러낸다. 공급사 호출 전 후보는 `RoomOption`, 가격·재고가 채워진 결과는 `SearchedRoom`. 공급사 코드(`stayCode`·`roomTypeCode`)는 응답 VO에 넣지 않는다.

## 자주 하는 실수
- "권장"만 보고 채택
- 공급사 DTO 외부 노출

## 스킬
/feature /test-write /push /git
