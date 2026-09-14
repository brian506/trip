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
| DB | H2 (실행·테스트 모두), Spring Data JPA |
| 계층 | `controller → business → implement → dataaccess` + `vo` |
| Implement | 저장소 단위 CRUD는 Manager 하나, 외부 호출은 Client |
| 도메인 검증 | VO 생성자에서 `AppException` |
| 응답·에러 | `ApiResponse`, `AppException`, `ErrorType`, `ErrorCode`, `ApiControllerAdvice` |
| 라이브러리 | Lombok (`lombok.config` 제약), springdoc-openapi |
| 공급사 Mock 서버 | `supplier` 패키지, `@Profile("supplier")`, 9090 포트 별도 프로세스 |
| 기록 | 저장소가 원본. 근거·과정은 `JOURNAL.md`, 이슈에는 요약과 링크 |
| 커밋 | `type[#이슈번호]: 설명`, 이슈는 사용자가 닫는다. JOURNAL·설계 기록은 해당 구현·테스트 커밋에 함께 넣는다 |
| 테스트 작성 | `@DisplayName` 자연어가 명세. 정상·경계·예외 상황을 제시하고 사용자가 고른 것만 구현. 구현 로직을 보고 케이스·기댓값을 만들지 않음 |
| 푸시 전 검증 | 구현 `review`, 테스트 `test-review` 리뷰 게이트. pre-push 훅은 두지 않는다 |
| 머지 조건 | `main` 대상 PR은 CI `test` 통과 필수(branch protection). 문서(`docs/`, `*.md`)만 바뀐 PR은 테스트를 건너뛰고 통과. 작업 환경 파일은 관리자 권한으로 `main`에 직접 푸시 |
| 브랜치 | 최신 `main`에서 이슈별 `<type>/<이슈번호>` 브랜치 → PR → Merge commit으로 `main`. 머지 후 삭제하고, 같은 이슈를 다시 작업하면 같은 이름으로 새로 만든다. 작업 환경 파일은 이슈·PR 없이 `main`에 직접 커밋 |
| 테스트 종류 | 단위 테스트, 통합 테스트. 통합은 `@IntegrationTest`(`@Tag("integration")` + `@SpringBootTest`). `test`는 단위, `integrationTest`는 통합. 통합은 `SpringTest` 상속(H2 인메모리, `@Transactional` 롤백), 공급사 호출은 MockWebServer, 단위는 BDDMockito·AssertJ. 위치·형식은 `test-write/references/test-conventions.md` |
| CI | GitHub Actions. `main` 대상 PR에서 한 job으로 단위·통합 테스트 step 분리 |
| 요금 표준 | 숙박 전체 총액, 세금 포함(gross) 하나. A는 `Σ(nightlyRate+taxAmount)`로 계산, B는 `totalPrice` 그대로. 날짜별 단가·세액은 표준에서 제외(한쪽만 제공). 다른 공급사 값으로 대체·추정하지 않는다 |
| 재고 표준 | 날짜별 `remainingRooms`는 둘 다 제공하므로 유지. N박 예약 가능 객실 수 = 날짜별 최솟값 |
| 조식 | `breakfastIncluded`는 표준 모델 필수 필드 (둘 다 제공, 비교 조건) |
| 실패 판정 | 두 어댑터 모두 HTTP 상태 → 본문 파싱 → 본문 코드 순으로 검사. 공급사 코드는 어댑터 안에 두고 도메인에는 내부 실패 분류(`BAD_REQUEST`·`AUTH`·`RATE_LIMITED`·`UNAVAILABLE`·`INTERNAL`·`MALFORMED`) + 원본 코드 문자열만 전달. 재시도 여부는 이 분류로 결정 |
| 병렬 호출 | 공급사 호출 태스크를 가상 스레드 Executor(`newVirtualThreadPerTaskExecutor`)에 제출, `invokeAll(tasks, 전체 예산)`으로 수집. 각 태스크 안에서 WebClient 체인(`.timeout()`, `.retryWhen()`)을 `.block()`. Business·Implement에 `Mono`를 올리지 않는다. 톰캣도 가상 스레드(`spring.threads.virtual.enabled=true`). `.block()`은 가상 스레드에서만 호출. `StructuredTaskScope`는 preview라 미사용 |
| 내부 식별자 | UUID, 앱에서 생성(`@UuidGenerator`). 순번 ID의 정렬 이점은 비범위(정렬·페이징)라 없고, 예약 흐름이 붙으면 열거 가능한 ID가 통로가 되므로 처음부터 불투명하게. 안정성은 유일 제약 + upsert가 보장 |
| H2 모드 | 실행은 파일(재시작 후에도 매핑·식별자 유지), 테스트는 인메모리 |
| 스키마 | `schema.sql` + `ddl-auto: validate`. DDL이 파일로 남고 엔티티와 어긋나면 기동 실패 |
| 매핑 엔티티 | `stay`(공급사, 공급사 숙소 코드, 이름, active, synced_at), `room_type`(stay FK, 공급사 객실 코드, 이름, 최대 인원, active, synced_at). 유일 키 `(supplier, supplier_hotel_code)`, `(stay_id, supplier_room_type_code)`. 목록에서 사라지면 삭제 대신 `active=false` |
| 도메인 패키지 | `com.trip.stay` — 매핑과 검색을 한 도메인에 |
| 트랜잭션 경계 | 매핑 조회만 짧은 read-only 트랜잭션, 공급사 호출은 트랜잭션 밖. `spring.jpa.open-in-view=false`. 가상 스레드는 스레드를 놓지 커넥션을 놓지 않으므로 둘 다 필요 |

## 미결정

- 공급사 요청·응답 DTO의 위치
- 타임아웃 값(연결·응답·전체 예산)과 재시도 횟수·백오프, 서킷 브레이커 채택 여부
- 부분 실패 표현 단위 (공급사 단위 vs 50개 배치 단위)
- 매핑에 없는 객실이 조회 응답에 왔을 때 처리 (버리고 기록 vs 즉시 매핑 생성)
- 표준 모델에 `taxAmount`(A만 제공) 포함 여부
- 통화: KRW만 가정할지
- 커밋 단위 기준
- `docs/` 구성
- 매핑 동기화 시점
