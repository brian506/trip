# 아키텍처

설계를 **왜** 이렇게 했는지는 [README 설계 의사결정](../README.md#설계-의사결정)에 있다. 이 문서는 **무엇이 어디에 있고 어떻게 흐르는지**를 정리한다.

## 1. 구성

```
[trip :8080]
  고객 → StayController ─┐
  스케줄러 → StaySyncScheduler ─┴→ StayService
                                   ├→ StayManager → H2 (stay, room_type)
                                   ├→ SupplierDispatcher → SupplierCircuitBreaker ─┐   (검색)
                                   └→ SupplierRetry ───────────────────────────────┤   (목록 동기화)
                                                                                   ↓
                                             SupplierAClient / SupplierBClient → SupplierHttpCaller (WebClient)
                                                                                   ↓ HTTP
[mock-supplier :9090]  /a/v1/hotels, /a/v1/availability, /b/api/properties, /b/api/search
```

| 모듈 | 역할 |
|---|---|
| `trip` (루트) | 검색 API, 숙소 목록 동기화, 공급사 연동 |
| `mock-supplier` | 공급사 A·B를 흉내 내는 별도 Spring Boot 앱. 정상 / 장애 / 무응답 모드 전환 가능 ([api.md](api.md#3-mock-공급사-제어)) |

## 2. 패키지와 의존 방향

```
com.trip
├── stay/            숙소 도메인
│   ├── controller/      StayController, StaySyncScheduler, request/response
│   ├── business/        StayService — 검색·동기화 흐름 조립
│   ├── implement/       StayManager, RoomTypeManager — 매핑 저장·조회, 결과 결합
│   ├── dataaccess/      Stay, RoomType 엔티티와 리포지토리
│   └── vo/              RoomOption(호출 전 후보), SearchedRoom(결과), RoomKey
├── supplier/        공급사 연동 경계
│   ├── SupplierClient   공급사 1곳의 계약 (fetchStays, fetchRooms)
│   ├── a/ b/            공급사별 Client와 응답 DTO — 이 안에서만 공급사 형식을 앎
│   ├── global/          SupplierHttpCaller, SupplierDispatcher, SupplierCircuitBreaker, SupplierRetry
│   └── vo/              SupplierStay, SupplierRoomType, SupplierRoom, SupplierFailure, SupplierStayCodes
├── support/         공통 예외(ErrorType, SupplierFailureType), 응답(ApiResponse), 값 타입(StayPeriod, Guests)
├── common/          ApiControllerAdvice
└── config/          WebClient, Executor, Swagger
```

* 도메인 계층은 `controller → business → implement → dataaccess` 한 방향.
* 도메인 → `supplier` 한 방향. `supplier`는 `stay`를 모른다.
* 공급사 응답 DTO(`a/response`, `b/response`)는 `SupplierStay`·`SupplierRoom`으로 변환된 뒤에만 밖으로 나간다.

## 3. 흐름

### 3.1 숙소 목록 동기화 (사전 작업)

트리거: 앱 기동 1회(`ApplicationRunner`) · 매일 04:00(`stay.sync.cron`) · 수동 `POST /api/v1/stays/sync`

```
StayService.syncAll()
 └─ 공급사마다 (순차)
     ├─ SupplierRetry.call(client::fetchStays)       최초 1회 + 재시도 2회, 2s → 6s 백오프
     │    └─ 실패(UNAVAILABLE·INTERNAL만 재시도) → warn 로그, 기존 매핑 유지하고 다음 공급사
     ├─ 항목 검증 실패 → 그 공급사 전체 건너뜀 (기존 매핑 유지)
     ├─ 빈 목록 → 건너뜀 (전부 비활성화되는 것을 막음)
     └─ StayManager.sync()                           @Transactional
          ├─ (supplier, stay_code)로 기존 행 조회 → 있으면 이름 갱신·재활성화, 없으면 생성
          ├─ 목록에서 사라진 숙소 → active=false
          └─ RoomTypeManager.sync() — 같은 방식으로 (stay_id, room_type_code)
```

### 3.2 통합 검색

```
GET /api/v1/stays/search
 └─ StayService.search(period, guests)
     ├─ StayManager.findActiveRooms(guests)          DB: 활성 숙소 + 활성이고 인원을 수용하는 객실 타입 → RoomOption[]
     │    └─ 후보 없음 → 빈 결과 200
     ├─ StayManager.toStayCodes()                    공급사별 숙소 코드 묶음
     ├─ SupplierDispatcher.dispatch()
     │    ├─ 공급사마다 가상 스레드 태스크 1개 — invokeAll(전체 5s)
     │    │    └─ 50개씩 나눈 묶음을 순차로
     │    │         └─ SupplierCircuitBreaker.call → client.fetchRooms → SupplierRoom[]
     │    │              ├─ 실패 → failures에 공급사당 첫 건만 기록
     │    │              └─ AUTH·RATE_LIMITED·UNAVAILABLE → 남은 묶음 중단
     │    └─ 5s 초과 태스크 취소 → UNAVAILABLE / TOTAL_TIMEOUT_EXCEEDED
     ├─ 모든 공급사 실패 → 502 (E2006, data=failures)
     └─ StayManager.toSearchedRooms()                (supplier, stayCode, roomTypeCode)로 후보와 결합 → SearchedRoom[]
          └─ 200 { rooms[], failures[] }
```

### 3.3 공급사 호출 한 번

```
SupplierXClient.fetchRooms()
 └─ SupplierHttpCaller.call(request, reader)
     ├─ exchangeToMono → .timeout(2s) → block()      가상 스레드에서만 block
     ├─ 전송 오류 → SupplierErrors.translate()        TIMEOUT, CONNECT_*, DNS, POOL_EXHAUSTED …
     └─ reader(status, body)                          공급사별 판정
          ├─ A: HTTP 4xx/5xx → SupplierErrors.classify(status), 본문 error를 원본 코드로
          └─ B: HTTP 200이어도 resultCode ≠ 0000 → BResultCode.classify()
```

실패는 모두 `SupplierCallException(supplier, type, code)` 하나로 올라온다. 분류 기준은 [README #16](../README.md#16-a의-4xx5xx-b의-resultcode-타임아웃-연결-실패를-하나의-분류로-묶은-이유는-분류-축을-무엇으로-잡았는가).

## 4. 스레드 모델

![재고/요금 검색의 가상 스레드 흐름](images/virtual-thread-flow.jpeg)

| 스레드 | 수 | 하는 일 |
|---|---|---|
| 요청 가상 스레드 | 검색 1건당 1 | DB 조회, `invokeAll()`로 최대 5s 대기, 결과 결합 |
| 공급사 태스크 가상 스레드 | 검색 1건당 공급사 수 | 묶음 순차 호출, `block()`에서 park |
| Netty 이벤트 루프 | 고정 | 소켓 I/O |

## 5. 견고성 설정

값은 모두 `application.yaml`의 `supplier.*`.

| 항목 | 값 | 적용 위치 |
|---|---|---|
| 연결 타임아웃 | 1s | `HttpClientSettings.withTimeouts()` |
| 응답 타임아웃 | 2s | `withTimeouts()` + `Mono.timeout()` |
| 검색 전체 상한 | 5s | `invokeAll(tasks, 5s)` |
| 커넥션 풀 | 공급사당 50, 대기 200ms, 유휴 30s | `ConnectionProvider` (공급사마다 별도) |
| 서킷 브레이커 (검색만) | 창 20, 최소 10, 실패율 50%, 열림 10s, 반열림 3 | `SupplierCircuitBreaker` |
| 재시도 (목록만) | 3회, 2s × 3 백오프 | `SupplierRetry` |

값의 근거: [README #15](../README.md#15-연결-1s응답-2s전체-5s는-어떻게-정한-값인가-개별-타임아웃과-별도로-전체-상한을-둔-이유는), [#20](../README.md#20-재시도를-숙소-목록-조회에만-건-이유는), [#21](../README.md#21-서킷-브레이커를-검색-경로에만-건-이유는)

## 6. 관찰 (로그·지표)

로그 형식, 현재 남기는 로그 목록, 지표 설계는 [README #22](../README.md#22-연동-상태를-무엇으로-관찰하는가-지금의-실패-로그만으로-공급사별-성공률지연타임아웃-비율을-볼-수-있는가).

## 7. 확장

* **공급사 추가**: `SupplierClient` 구현 + enum + WebClient 빈 + 설정. 도메인·Dispatcher는 그대로 ([README #13](../README.md#13-신규-공급사를-추가할-때-어디를-고쳐야-하는가))
* **숙소 수천 개**: 필터 → 기준 정렬 → 상한까지만 호출해 검색 1건의 요청 수를 제한. 캐시는 그 위의 선택 사항 ([README #23](../README.md#23-요금재고를-캐시한다면-어디까지-얼마나-오래-둘-것인가)) ([README #9](../README.md#9-숙소-코드-50개-제한-아래에서-공급사는-병렬-묶음은-순차로-부르는-이유는-숙소가-수천-개가-되면))
