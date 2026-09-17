# trip

여러 숙박 공급사의 상품을 하나의 모델로 통합해, 날짜와 인원으로 한 번에 검색하는 API 서버.

## 개요

숙박 공급사는 저마다 다른 방식으로 상품을 표현한다. 같은 객실이라도 A는 1박 요금과 세액을 날짜별로 나눠 주고, B는 기간 전체 총액을 세금 포함으로 한 번에 준다. 실패를 알리는 방법도 다르다. A는 HTTP 상태 코드로 알리지만, B는 장애 상황에서도 HTTP 200을 주고 본문 `resultCode`로만 알린다.

이 서버는 그 차이를 바깥으로 흘리지 않는다. 고객은 날짜와 인원만 보내고, 공급사가 몇 곳이든 같은 모양의 결과를 받는다.

- **통합 모델** — 공급사 코드를 내부 식별자(UUID)로 바꿔 돌려준다. 같은 공급사 상품은 언제 조회해도 같은 식별자로 나온다.
- **부분 실패 허용** — 공급사 한 곳이 죽어도 나머지 결과로 응답한다. 실패한 공급사는 응답의 `failures[]`에 사실대로 남는다.
- **시간 상한** — 공급사 호출을 가상 스레드로 병렬 처리하고, 검색 한 건이 쓸 수 있는 전체 시간을 5초로 묶는다.
- **공급사 추가** — 클라이언트 구현 하나와 설정 한 블록이면 늘어난다. 도메인 코드는 건드리지 않는다.

## 기술 스택

| 구분 | 사용 |
|---|---|
| 언어·런타임 | Java 25 (가상 스레드) |
| 프레임워크 | Spring Boot 4.1 (MVC) |
| 외부 호출 | Spring WebClient (Reactor Netty) |
| 영속성 | Spring Data JPA, H2 (파일 모드) |
| 견고성 | Resilience4j (서킷 브레이커·재시도) |
| 문서화 | springdoc-openapi 3.0.0 |
| 빌드 | Gradle 멀티 모듈 |

## 빠른 시작

JDK 25가 필요하다. 공급사는 같은 저장소의 Mock 모듈을 호출한다.

```bash
# 1) 공급사 Mock (9090) — 먼저 띄운다. 애플리케이션이 기동하며 숙소 목록을 동기화한다.
./gradlew :mock-supplier:bootRun

# 2) 애플리케이션 (8080)
./gradlew bootRun
```

```bash
# 3) 검색
curl "http://localhost:8080/api/v1/stays/search?checkIn=2026-10-01&checkOut=2026-10-04&adults=2&children=0"
```

```json
{
  "resultType": "SUCCESS",
  "data": {
    "rooms": [
      {
        "stayId": "c8d040b2-1172-4216-b99e-16275c2aa394",
        "stayName": "Riverside Hotel Seoul",
        "roomTypeId": "6b1b3e20-bf61-4e28-9d15-bac57debdb15",
        "roomTypeName": "Deluxe Twin Room",
        "maxOccupancy": 2,
        "availableRooms": 1,
        "supplier": "B",
        "breakfastIncluded": true,
        "currency": "KRW",
        "totalPrice": 452000
      },
      {
        "stayId": "aecf1f36-ced7-48f2-98b6-833b35af21fe",
        "stayName": "Riverside Hotel Seoul",
        "roomTypeId": "f1f79e08-b377-42d0-b57a-dba7c7dadb5a",
        "roomTypeName": "Deluxe Twin",
        "maxOccupancy": 2,
        "availableRooms": 1,
        "supplier": "A",
        "breakfastIncluded": false,
        "currency": "KRW",
        "totalPrice": 429000
      },
      {
        "stayId": "069aab19-b04b-4399-be6d-9922710d186c",
        "stayName": "Namsan Garden Stay",
        "roomTypeId": "84e33ebd-c47d-43d2-8430-058024d5c357",
        "roomTypeName": "Standard Double",
        "maxOccupancy": 2,
        "availableRooms": 0,
        "supplier": "A",
        "breakfastIncluded": false,
        "currency": "KRW",
        "totalPrice": 302500
      }
    ],
    "failures": []
  },
  "error": null
}
```

같은 숙소(`Riverside Hotel Seoul`)가 공급사 A와 B에서 각각 한 건씩 나온다. 서로 다른 상품으로 다루기 때문이고, 조식 포함 여부와 총액이 달라 고객이 비교할 수 있다. 세 번째 항목은 숙박 기간 중 재고가 없는 날이 있어 `availableRooms`가 0이다.

- Swagger UI — <http://localhost:8080/swagger-ui.html>
- OpenAPI 문서 — <http://localhost:8080/v3/api-docs>
- H2 파일은 `./data/trip.mv.db`로 만들어진다. 스키마는 `src/main/resources/schema.sql`이고 JPA는 `validate`만 한다.

### 빌드와 테스트

```bash
./gradlew build            # 컴파일 + 단위 테스트 + 통합 테스트
./gradlew test             # 단위 테스트
./gradlew integrationTest  # @IntegrationTest 통합 테스트
```

## API

모든 응답은 `resultType` / `data` / `error` 봉투에 담긴다. 성공이면 `error`가, 실패면 `data`가 `null`이다.

| 메서드 | 경로 | 설명 | 성공 상태 |
|---|---|---|---|
| `GET` | `/api/v1/stays/search` | 날짜·인원으로 통합 검색 | 200 |
| `POST` | `/api/v1/stays/sync` | 숙소 목록 수동 동기화 | 201 |

### `GET /api/v1/stays/search`

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `checkIn` | `yyyy-MM-dd` | O | 오늘 이후 |
| `checkOut` | `yyyy-MM-dd` | O | `checkIn` 이후, 최대 30박 |
| `adults` | `int` | O | 1 이상 |
| `children` | `int` | | 0 이상 (기본 0) |

`data.rooms[]` 한 건이 팔 수 있는 객실 하나다.

| 필드 | 설명 |
|---|---|
| `stayId` · `stayName` | 내부 숙소 식별자와 이름 |
| `roomTypeId` · `roomTypeName` | 내부 객실 타입 식별자와 이름 |
| `maxOccupancy` | 최대 수용 인원 |
| `availableRooms` | 숙박 기간 전체에 예약 가능한 객실 수. **0이면 예약 불가** |
| `supplier` | 출처 공급사 |
| `breakfastIncluded` | 조식 포함 여부 |
| `currency` · `totalPrice` | 통화와 숙박 기간 총액(세금 포함) |

`data.failures[]`에는 호출에 실패한 공급사가 담긴다. 비어 있으면 모든 공급사가 정상 응답했다는 뜻이다.

```json
{ "supplier": "A", "type": "UNAVAILABLE", "code": "SERVICE_UNAVAILABLE" }
```

`type`은 공급사와 무관한 내부 실패 분류(`BAD_REQUEST` `AUTH` `RATE_LIMITED` `UNAVAILABLE` `INTERNAL` `MALFORMED`)이고, `code`는 원인을 좁히기 위한 원본 코드다.

### 오류 코드

| 코드 | 상태 | 뜻 |
|---|---|---|
| `E1001` | 400 | 체크인·체크아웃 날짜가 없거나 순서가 뒤바뀜 |
| `E1002` | 400 | 체크인이 오늘 이전 |
| `E1003` | 400 | 30박 초과 |
| `E1004` | 400 | 인원 값이 올바르지 않음 |
| `E2000`~`E2005` | 502 | 공급사 호출 실패 (요청·인증·한도·연결·내부 오류·응답 해석) |
| `E2006` | 502 | 모든 공급사 호출이 실패 |

전체 목록은 [docs/api.md](docs/api.md)에 있다.

## 프로젝트 구조

```
trip/
├── src/main/java/com/trip/
│   ├── stay/          숙소 도메인 (검색·동기화)
│   │   ├── controller/    요청 검증, 응답 변환
│   │   ├── business/      흐름 조립 (StayService)
│   │   ├── implement/     매핑 저장·조회 (StayManager, RoomTypeManager)
│   │   ├── dataaccess/    엔티티와 리포지토리
│   │   └── vo/            RoomOption → SearchedRoom
│   ├── supplier/      공급사 연동 경계
│   │   ├── a/ b/          공급사별 클라이언트와 응답 DTO
│   │   ├── global/        공통 호출기, 병렬 실행, 서킷 브레이커, 재시도
│   │   └── vo/            공급사 입력 모델
│   ├── support/       공통 예외·응답·값 타입
│   └── config/        WebClient, Executor, Swagger
└── mock-supplier/     공급사 A·B Mock (별도 모듈, 9090)
```

계층은 `controller → business → implement → dataaccess` 한 방향이고, 공급사 연동은 `com.trip.supplier`로 떼어 **도메인 → supplier 한 방향**으로만 의존한다. 공급사 응답 DTO는 `supplier` 패키지 밖으로 나가지 않는다.

자세한 구조는 [docs/architecture.md](docs/architecture.md)를 참고한다.

## 데이터 모델

저장하는 것은 **공급사 코드와 내부 식별자의 매핑**뿐이다. 요금과 재고는 원본이 공급사에 있으므로 저장하지 않고 검색할 때마다 가져온다.

```
stay                                room_type
├─ stay_id      (UUID, PK)          ├─ room_type_id    (UUID, PK)
├─ supplier     ┐ UNIQUE            ├─ stay_id         ┐ UNIQUE
├─ stay_code    ┘                   ├─ room_type_code  ┘
├─ name                             ├─ name
└─ active                           ├─ max_occupancy
                                    └─ active
```

숙소와 객실 타입 2단계로 나눴고, 각각 `(공급사, 코드)`와 `(숙소, 코드)`에 유니크 제약이 있어 같은 공급사 상품은 항상 같은 UUID로 돌아온다. 공급사 목록에서 사라진 항목은 지우지 않고 `active`를 내린다.

공급사별 필드가 어떻게 표준 모델로 접히는지는 [docs/integration-model.md](docs/integration-model.md)에 표로 정리했다.

## 장애 상황 확인

Mock은 공급사별로 세 가지 모드를 지원한다. 애플리케이션을 띄운 채로 모드만 바꾸면 된다.

```bash
curl -X POST "http://localhost:9090/control/a/mode?value=error"        # normal | error | no-response
```

| 모드 | 공급사 A | 공급사 B |
|---|---|---|
| `normal` | 정상 응답 | 정상 응답 |
| `error` | HTTP 503 | HTTP 200 + `resultCode: E503` |
| `no-response` | 응답하지 않음 | 응답하지 않음 |

A만 `error`로 바꾸면 B의 결과만 돌아오고 실패 사실이 함께 담긴다.

```json
{ "rooms": [ ... 1건 ... ],
  "failures": [ { "supplier": "A", "type": "UNAVAILABLE", "code": "SERVICE_UNAVAILABLE" } ] }
```

B도 `error`로 바꾸면 502가 된다. B는 HTTP 200으로 실패를 알리지만 A의 503과 같은 `UNAVAILABLE`로 다룬다.

```json
{ "errorCode": "E2006",
  "message": "모든 공급사 호출이 실패했습니다.",
  "data": [ { "supplier": "A", "type": "UNAVAILABLE", "code": "SERVICE_UNAVAILABLE" },
            { "supplier": "B", "type": "UNAVAILABLE", "code": "E503" } ] }
```

`no-response`는 응답 타임아웃(2초)에 걸려 `code`가 `TIMEOUT`이 된다. 서킷 브레이커가 열린 뒤에는 호출 없이 즉시 접어 `CIRCUIT_OPEN`이 된다.

## 설정

모든 값은 `src/main/resources/application.yaml`의 `supplier.*`와 `stay.*`에 있다.

| 키 | 값 | 설명 |
|---|---|---|
| `supplier.endpoints.{A\|B}` | `base-url`, `api-key` | 공급사별 접속 정보 |
| `supplier.connect-timeout` | `1s` | 연결 |
| `supplier.response-timeout` | `2s` | 응답 |
| `supplier.total-timeout` | `5s` | 검색 한 건이 공급사 호출에 쓰는 전체 시간 |
| `supplier.max-connections` | `50` | 공급사별 커넥션 풀 크기 |
| `supplier.pending-acquire-timeout` | `200ms` | 풀 대기 |
| `supplier.max-idle-time` | `30s` | 유휴 커넥션 정리 |
| `supplier.circuit-breaker.*` | 창 `20` / 최소 `10` / `50%` / 열림 `10s` / 반열림 `3` | 검색 경로 서킷 브레이커 |
| `supplier.retry.*` | `3`회 / `2s` / 배수 `3` | 동기화 재시도 |
| `stay.sync.enabled` | `true` | 동기화 켜기 |
| `stay.sync.cron` | `0 0 4 * * *` | 기동 시 1회 + 매일 04:00 |

값을 그렇게 정한 근거는 아래 설계 의사결정에 적는다.

## 설계 의사결정

> 작성 예정. 아래 항목을 다룬다.
>
> - 통합 모델에서 무엇을 표준으로 삼고 무엇을 버렸는지
> - 예약 불가 상품을 `availableRooms: 0`으로 노출한 이유
> - 연박(N박) 예약 가능 객실 수 판정
> - 매핑을 언제 호출하는지와 그 근거
> - 신규 공급사를 추가할 때 고쳐야 하는 것
> - 타임아웃 값을 그렇게 정한 근거
> - 숙소가 수천 개로 늘었을 때
> - 재시도와 서킷 브레이커를 서로 다른 경로에 건 이유
> - MVC 위에서 WebClient를 쓰고 WebFlux를 도입하지 않은 이유

## 문서

- [docs/architecture.md](docs/architecture.md) — 계층 구조, 패키지 경계, 검색 요청의 호출 흐름
- [docs/integration-model.md](docs/integration-model.md) — 통합 모델과 공급사별 필드 매핑
- [docs/api.md](docs/api.md) — 응답 계약과 오류 코드 전체
- [JOURNAL.md](JOURNAL.md) — 개발 과정의 의사결정, 막힌 지점, AI 활용 기록
