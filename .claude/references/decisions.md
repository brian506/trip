# 결정 현황

사용자가 확정한 것만 구현에 반영한다. 미결정 항목은 선택지와 트레이드오프를 정리해 묻고, 결정되면 이 파일을 갱신한다.
기획 문서의 "권장" 항목이라는 이유만으로 채택하지 않는다. 필수 요구사항만 전제로 둔다.

## 확정

| 항목 | 결정 |
|------|------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1, Spring MVC |
| Build | Gradle Groovy DSL, 멀티 모듈(루트 = 앱, `mock-supplier` = 공급사 Mock) |
| 외부 호출 | Spring WebClient |
| DB | H2 (실행·테스트 모두), Spring Data JPA |
| 계층 | `controller → business → implement → dataaccess` + `vo` |
| Implement | 저장소 단위 CRUD는 Manager 하나, 외부 호출은 Client |
| 도메인 검증 | VO 생성자에서 `AppException` |
| 응답·에러 | `ApiResponse`, `AppException`, `ErrorType`, `ErrorCode`, `ApiControllerAdvice` |
| 라이브러리 | Lombok (`lombok.config` 제약), springdoc-openapi |
| 공급사 Mock 서버 | 별도 Gradle 모듈 `mock-supplier`, 9090 포트 별도 프로세스 |
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
| 실패 판정 | 두 어댑터 모두 HTTP 상태 → 본문 파싱 → 본문 코드 순으로 검사. 공급사 코드는 어댑터 안에 두고 도메인에는 내부 실패 분류(`BAD_REQUEST`·`AUTH`·`RATE_LIMITED`·`UNAVAILABLE`·`INTERNAL`·`MALFORMED`) + 원본 코드 문자열만 전달. 재시도 여부는 이 분류로 결정. 판정은 WebClient 체인(`onStatus`, Netty 스레드)이 아니라 `block()` 이후 호출 스레드의 동기 코드에서 한다: 응답을 `exchangeToMono(toEntity(String))`로 상태·본문 문자열째 받고, Client가 상태 분기와 JSON 파싱(Boot `JsonMapper`)을 한다 |
| 공급사 호출 구조 | 조합. 공통 실행기 `SupplierHttpCaller`(`call`: 요청 실행·`.timeout()`·`block()`·전송 오류 변환, `parse`: JSON 파싱과 `EMPTY_BODY`·`UNPARSEABLE`)를 각 Client가 주입받아 쓰고, Client는 요청 조립과 응답 판정(reader)만 맡는다. 추상 베이스 클래스·데코레이터는 채택하지 않음. 모니터링·재시도를 붙이면 이 실행기에 붙인다 |
| 공급사 설정·WebClient | 설정은 yml `supplier.endpoints.{Supplier enum 이름}` → `Map<Supplier, SupplierEndpoint>`, 없으면 기동 실패. WebClient는 공급사별 빈(`supplierX`, Client가 `@Qualifier`로 주입). Boot 자동 구성 `WebClient.Builder`를 `clone()`해서 만든다. 팩토리 방식은 채택하지 않음(명시성·공급사별 조정·테스트 편의를 우선, C 추가 시 빈 메서드 하나 추가는 감수) |
| 병렬 호출 | 공급사 호출 태스크를 가상 스레드 Executor(`newVirtualThreadPerTaskExecutor`)에 제출, `invokeAll(tasks, 전체 예산)`으로 수집. 각 태스크 안에서 WebClient 체인(`.timeout()`)을 `.block()`. Business·Implement에 `Mono`를 올리지 않는다. 톰캣도 가상 스레드(`spring.threads.virtual.enabled=true`). `.block()`은 가상 스레드에서만 호출. `StructuredTaskScope`는 preview라 미사용 |
| 내부 식별자 | UUID, 앱에서 생성(`@UuidGenerator`). 순번 ID의 정렬 이점은 비범위(정렬·페이징)라 없고, 예약 흐름이 붙으면 열거 가능한 ID가 통로가 되므로 처음부터 불투명하게. 안정성은 유일 제약 + upsert가 보장 |
| H2 모드 | 실행은 파일(재시작 후에도 매핑·식별자 유지), 테스트는 인메모리 |
| 스키마 | `schema.sql` + `ddl-auto: validate`. DDL이 파일로 남고 엔티티와 어긋나면 기동 실패 |
| 매핑 엔티티 | `stay`(PK stay_id, supplier, stay_code, name, active), `room_type`(PK room_type_id, stay_id, room_type_code, name, max_occupancy, active). PK 컬럼명은 `id` 대신 `<테이블>_id`. 컬럼명은 공급사 용어(hotel/property) 없이 중립으로. 유일 키 `(supplier, stay_code)`, `(stay_id, room_type_code)`. 목록에서 사라지면 삭제 대신 `active=false`(양쪽 테이블 모두). JPA 연관 매핑 없이 `stay_id` UUID 컬럼 + DDL FK 제약만, cascade 없음 |
| 고정값 출처 | 숙소명·객실 타입명·최대 인원은 목록 API 스냅샷을 DB에 저장하고 응답에 DB 값을 쓴다. 재고·요금 API가 주는 같은 값은 무시. 요금·재고만 매 요청 공급사에서 |
| 공통 시각 컬럼 | `BaseEntity`(`@MappedSuperclass`)에 `created_at`·`updated_at`(Spring Data JPA Auditing: `@CreatedDate`/`@LastModifiedDate`, `@EnableJpaAuditing`)만 둔다. 동기화는 목록 값이 DB와 다를 때만(이름·최대 인원 변경, 재활성화) 엔티티 `applyLatestInfo`가 필드를 바꾸고, 값이 같으면 건드리지 않아 UPDATE가 나가지 않는다(매 동기화마다 모든 행을 쓰지 않기 위함). 그래서 `updated_at`이 실제 변경 시각이 되고, 별도 `last_synced_at`은 `updated_at`과 겹쳐 두지 않는다. 엔티티 생성은 생성자에 `@Builder`. 타입은 `LocalDateTime` ↔ H2 `TIMESTAMP` |
| 매핑 없는 객실 | 재고·요금 응답에 매핑에 없는 객실 코드가 오면 그 항목은 응답에서 빼고 로그만 남긴다. 검색 경로에는 쓰기를 두지 않으며, 다음 주기 동기화가 반영한다. 임계값 트리거는 숫자 근거가 없고 검색 빈도를 재게 되어 채택하지 않음 |
| 매핑 조회 | 동기화: `StayRepository.findAllBySupplier`(비활성 포함), `RoomTypeRepository.findAllByStayIdIn`. 검색: 활성 숙소만 읽고, `room_type`은 active 조건 없이 `findAllByStayIdIn`으로 읽는다(팔 객실은 재고·요금 API가 정하므로 번역만 한다). 검색용 활성 숙소 조회 메서드는 쓰는 곳이 없어 제거했고 검색 구현 때 추가한다. 건별 코드 조회 대신 목록을 읽어 메모리에서 대조. 숙소가 통째로 빠져도 객실 `active`는 내리지 않는다(`stay.active`로 충분) |
| Manager 구성 | 저장소마다 하나: `StayManager`(`StayRepository`), `RoomTypeManager`(`RoomTypeRepository`). 현재 메서드는 동기화 `sync`(쓰기 트랜잭션)뿐이고, 검색용 조회(read-only)는 검색 구현 때 추가한다. 숙소·객실 타입 동기화는 원자적이어야 하므로 `StayManager.sync`가 같은 트랜잭션 안에서 `RoomTypeManager.sync`를 부른다(Implement 안 협력). Manager는 엔티티를 밖으로 내지 않고 VO로 변환해 반환 |
| 동기화 VO | 목록 API 입력: `SupplierStay`(stayCode, name, roomTypes), `SupplierRoomType`(roomTypeCode, name, maxOccupancy). 빈 코드·이름, 최대 인원 1 미만은 `INVALID_SUPPLIER_STAY`(E1000, 502). 검색 출력 VO(엔티티 `from`)는 검색 구현 때 추가한다. 목록에 같은 코드가 두 번 오면 첫 건만 쓰고 뒤 건은 로그 없이 건너뛴다. 검증 실패 시 `AppException` data에 코드와 사유(`stayCode=H1, 숙소 이름 없음`, `roomTypeCode=R1, 최대 인원 1 미만 (0)`)를 담고, 동기화 건너뜀 로그에 첫 오류만 남긴다(전체를 건너뛰므로 오류 개수는 세지 않음). 객실 VO는 어느 숙소의 객실인지 모른다 |
| 동기화 쓰기 배치 | JDBC 배치 `hibernate.jdbc.batch_size=50` + `order_updates=true`. 크기 50은 잠정값(공급사 목록 전체를 커밋 때 쓰므로 조회 단위와 무관). 서버형 DB로 옮기면 드라이버 멀티 row 재작성 지원·네트워크 지연·행 크기로 다시 정한다. `@UuidGenerator`(앱 생성 ID)라 INSERT도 배치된다(IDENTITY면 불가). `order_inserts`는 끈다: 연관 매핑이 없어 Hibernate가 FK 순서를 모르고, 숙소를 먼저 저장하므로 이미 종류별로 모여 있다 |
| 타임아웃 | 연결 1s, 응답 3s, 검색 전체 예산 5s (`supplier.*`). 고객 체감 한계를 5초로 보고, 한 공급사가 늦어도 나머지로 5초 안에 응답. 커넥터(`HttpClientSettings`)에 연결·읽기, Mono `.timeout()`에 응답, `invokeAll(tasks, 예산)`에 전체. 예산 초과 태스크는 취소되고 `UNAVAILABLE/BUDGET_EXCEEDED` |
| 부분 실패 표현 | 공급사 단위. 응답 `failures[]`에 (supplier, type, code) 한 건. 50개 묶음 중 일부만 실패해도 그 공급사를 실패로 표시하되 성공한 묶음의 상품은 그대로 응답 |
| 동기화 시점 | 기동 시 1회(`ApplicationRunner`) + 매일 04:00(`@Scheduled`, `stay.sync.cron`) + 수동 `POST /api/v1/stays/sync`. 진입점은 `StaySyncScheduler`(controller 패키지). `stay.sync.enabled=false`로 끔(테스트). 기동 시 공급사가 죽어 있어도 기동은 계속. 목록 호출 성공인데 비어 있으면 응답 이상으로 보고 warn 로그를 남긴 뒤 건너뜀(전체 비활성화 안 함) |
| 동기화 항목 오류 | 목록 응답 중 한 항목이라도 검증(`INVALID_SUPPLIER_STAY`)에 실패하면 그 공급사 동기화 전체를 건너뛰고 기존 DB 값을 유지한다. 잘못된 항목만 빼고 반영하는 방식은 채택하지 않음: 빠진 숙소가 목록에 없는 숙소로 처리돼 비활성화되면 응답 오류 때문에 판매 중인 숙소가 검색에서 사라진다. 목록 데이터는 잘 바뀌지 않아 다음 주기(최대 하루) 반영으로 충분. 검색 경로의 항목 단위 제외(매핑 없는 객실, 날짜 누락)는 저장이 없어 그대로 둔다 |
| 동기화 저장 실패 격리 | 공급사마다 `StayManager.sync` 예외를 잡아 error 로그(예외 포함)를 남기고 다음 공급사로 진행한다. 트랜잭션이 공급사마다 분리돼 실패한 공급사만 롤백된다. DB 저장 재시도는 넣지 않음: 제약 위반 같은 결정적 오류는 같은 실패만 반복하고, 재시도를 다 써도 격리는 따로 필요하며, 내장 H2라 일시적 오류가 드물다. 재시도는 미결정 항목과 함께 정한다 |
| 로그 형식 | `[카테고리 : 상세내용]: key=value \| key=value`로 통일(`CLAUDE.md`). 레벨은 정상 흐름 `info`, 예상된 실패 `warn`, 시스템 오류 `error`. 동기화를 건너뛸 때는 사유마다 로그를 남긴다: 목록 조회 실패·항목 검증 실패·빈 목록은 `warn`, 저장 실패·스케줄 실행 실패는 `error`(예외 포함) |
| 외부 연동 패키지 | 공급사 연동 코드는 최상위 `com.trip.supplier`에 두되 역할별 하위 패키지로 나눈다: 루트는 계약(`SupplierClient`·`Supplier`)만, `vo/`는 입력 모델(`SupplierStay`·`SupplierRoomType`), `exception/`은 `SupplierCallException`·`SupplierFailureType`·`SupplierErrors`, `infra/`는 `SupplierHttpCaller`·`SupplierProperties`·`SupplierEndpoint`. `WebClientConfig`는 설정 클래스이므로 `com.trip.config.supplier`로 옮겨 `SwaggerConfig`와 나란히 둔다. 공급사별은 `a/`·`b/`에 Client, 코드 매핑, `response/`. 의존은 도메인 → `supplier` 한 방향(Business·Manager·엔티티가 `supplier`를 import, `supplier`는 도메인을 import하지 않음). 입력 모델을 `stay/vo`에 두면 `stay ⇄ supplier` 순환이 생겨 `supplier`로 옮김. `stay/implement`에는 Manager만 남는다. Implement 안에서 분리, 인터페이스만 도메인에 두는 의존성 역전은 채택하지 않음. 이름은 `failure/`·`http/` 대신 `exception/`·`infra/` |
| 공급사 예외 계층 | `SupplierCallException`은 `AppException`을 상속한다(별도 계층으로 두지 않음). `SupplierFailureType`의 각 값이 대응 `ErrorType`을 들고 있고(`SUPPLIER_BAD_REQUEST`~`SUPPLIER_MALFORMED`, `E2000`~`E2005`, 전부 502·WARN), 생성자가 `type.getErrorType()`을 `super`로 넘긴다. `data`에는 `supplier type code`를 담아 `ApiControllerAdvice`·로그에서 원본 코드를 본다. `AppException`에 `(ErrorType, data, cause)` 생성자를 추가했다. VO 검증 실패는 기존대로 `AppException(INVALID_SUPPLIER_STAY)`라서 `StaySyncService`는 `SupplierCallException`(호출 실패) → `AppException`(항목 검증) 순으로 잡는다 |
| 공급사 응답 형식 위치 | `supplier/{a,b}/response` 공급사별 하위 패키지. 클래스명 접두어(`A*`, `B*`)는 유지. 지금 담긴 8개가 전부 응답 파싱용이고 요청 형식은 없어서, 잡동사니 이름인 `dto/` 대신 도메인의 `controller/request`·`controller/response` 관례와 같은 이름을 쓴다. 공급사 요청 형식을 객체로 만들 일이 생기면 그때 `request/`를 만든다 |
| 공급사 코드 매핑 | 본문 코드가 있는 공급사는 전용 enum을 공급사 패키지에 둔다(`b/BResultCode`: 코드 → `SupplierFailureType`, 코드 없음 `MALFORMED`, 모르는 코드 `INTERNAL`). 공급사 코드 enum을 `ErrorType`과 합치지 않음: 공급사 코드가 전역 enum에 섞이고, 공급사 실패는 대부분 에러 응답이 되지 않는다(검색 `failures[]`, 동기화 로그). 공급사 코드 → `SupplierFailureType` → `ErrorType` 두 단계로 두어 공급사 코드는 `a/`·`b/` 안에 갇힌다 |
| 어댑터 구성 | `SupplierClient` 인터페이스(`supplier()`, `fetchStays()`. 재고·요금 `fetchOffers()`는 검색 구현 때 추가)와 `SupplierAClient`·`SupplierBClient`. Business는 `List<SupplierClient>`를 주입받아 공급사 분기 없음. 실패는 `SupplierCallException`(supplier, type, 원본 code). 공통 분류는 `SupplierErrors`(HTTP 상태 분류, 전송 오류 변환: `TIMEOUT`·`CONNECTION`·`BODY_TOO_LARGE`·`UNEXPECTED`). 신규 공급사 추가 = enum 값 + Client 구현체(`SupplierHttpCaller` 조합) + `response/`(+ `toSupplierStay()`) + `config/supplier/WebClientConfig` 빈 메서드 + yml `supplier.endpoints` |
| 정규화 | A: 총액 `Σ(nightlyRate+taxAmount)`, B: `totalPrice`. 예약 가능 수 = 기간 내 날짜별 최솟값. 요청 기간의 날짜가 응답에 빠지면 그 항목은 버리고 경고 로그(총액·재고를 만들 수 없음). `taxAmount`는 표준에 없음. 통화는 변환 없이 코드 그대로 전달(현재 KRW뿐) |
| 검색 응답 | `stays[]`(stayId, stayName, roomTypeId, roomTypeName, maxOccupancy, availableRooms, available, supplier, breakfastIncluded, currency, totalPrice) + `failures[]`. 예약 불가는 `availableRooms=0`으로 노출하고 빼지 않는다 |
| Mock | 별도 Gradle 모듈 `mock-supplier`(`MockSupplierApplication` + `MockSupplierController`, webmvc 스타터만). 자체 `application.yaml`로 9090. 운영 앱과 코드·의존성·산출물이 분리돼 프로파일과 기능 끄기 설정이 필요 없다. 루트 앱은 `mock-supplier`를 의존하지 않고 HTTP로만 호출한다. 현재는 A·B 숙소 목록 고정 응답만 있다. 모드 전환(normal/error/no-response, `POST /control/{a|b}/mode?value=`)과 재고·요금 고정 날짜(2026-09-01~03) 응답은 검색 구현 때 추가 |
| 도메인 패키지 | `com.trip.stay` — 매핑과 검색을 한 도메인에 |
| 트랜잭션 경계 | 매핑 조회만 짧은 read-only 트랜잭션, 공급사 호출은 트랜잭션 밖. `spring.jpa.open-in-view=false`. 가상 스레드는 스레드를 놓지 커넥션을 놓지 않으므로 둘 다 필요 |

## 미결정

- 재시도 횟수·백오프, 서킷 브레이커 채택 여부 (선택 구현. 현재 미구현). 판정이 `block()` 이후로 옮겨가 체인 안 `.retryWhen()`으로는 B의 본문 코드 실패를 볼 수 없으므로, 붙이면 `SupplierHttpCaller` 단위로 건다
- 수동 동기화 API 결과 표현: 공급사 실패(호출·항목 오류·저장 실패)를 잡아 로그만 남기므로 전부 실패해도 201. 공급사별 결과를 응답에 담을지는 재시도를 정할 때 함께 정한다
- 공급사별 모니터링(성공률·응답 지연·타임아웃 비율) 설계·구현 (보류)
- 예약 불가 상품(availableRooms=0)을 응답에 포함(현재)할지 제외할지
- 커밋 단위 기준
- `docs/` 구성

## 고려 사항

결정 대상은 아니지만 근거가 생기면 다시 본다.

- 응답 버퍼 한도: WebClient는 응답을 기본 256KB까지만 메모리에 모은다. 넘으면 `MALFORMED/BODY_TOO_LARGE`로 그 공급사 동기화를 건너뛴다(숙소 약 3,000개 목록 ≈ 390KB에서 실패 확인). 실제 목록 데이터와 크기를 모르므로 값은 정하지 않는다
