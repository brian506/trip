# API 명세

Swagger UI: <http://localhost:8080/swagger-ui.html> · OpenAPI: <http://localhost:8080/v3/api-docs> (서버 실행 중)

아래 응답 예시는 로컬에서 Mock(9090)과 앱(8080)을 띄워 실제로 받은 값이다. UUID는 실행 환경마다 다르다.

## 공통 응답 형식

```json
{
  "resultType": "SUCCESS | ERROR",
  "data": { },
  "error": { "errorCode": "E2006", "message": "…", "data": { } }
}
```

| 필드 | 설명 |
|---|---|
| `resultType` | `SUCCESS` 또는 `ERROR` |
| `data` | 성공 시 결과. 실패 시 `null` |
| `error` | 실패 시 오류. `data`에는 오류의 상세(필드 오류, 공급사 실패 목록 등)가 들어간다. 성공 시 `null` |

---

## 1. 통합 검색

```
GET /api/v1/stays/search
```

보유한 활성 숙소 전체를 대상으로, 인원을 수용할 수 있는 객실 타입의 요금·재고를 모든 공급사에서 병렬로 조회해 하나의 목록으로 돌려준다.

### 요청 파라미터

| 이름 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `checkIn` | date (`yyyy-MM-dd`) | O | 오늘 이후 |
| `checkOut` | date (`yyyy-MM-dd`) | O | `checkIn` 이후, 최대 30박 |
| `adults` | int | O | 1 이상 |
| `children` | int | X | 0 이상, 기본 0 |

* 후보: 활성(`active=true`) 숙소의 활성 객실 타입 중 `maxOccupancy >= adults + children`인 것만 조회한다.
* 지역·키워드 필터, 정렬, 페이징은 없다.

### 응답 `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `rooms[]` | array | 검색 결과. 필드는 [domain-model.md §3](domain-model.md#3-검색-결과-모델--searchedroom) |
| `failures[]` | array | 실패한 공급사. **공급사당 최대 1건**. 비어 있으면 모든 공급사가 성공 |
| `failures[].supplier` | `A` \| `B` | 실패한 공급사 |
| `failures[].type` | string | 실패 분류: `BAD_REQUEST`, `AUTH`, `RATE_LIMITED`, `UNAVAILABLE`, `INTERNAL`, `MALFORMED` |
| `failures[].code` | string | 원본 코드 (`E503`, `SERVICE_UNAVAILABLE`, `TIMEOUT`, `CIRCUIT_OPEN`, `TOTAL_TIMEOUT_EXCEEDED` 등) |

* `availableRooms = 0`인 상품(기간 중 하루라도 매진)도 결과에 포함된다.
* 공급사 한 곳의 일부 묶음만 실패해도 그 공급사는 `failures[]`에 오르고, 성공한 묶음의 상품은 `rooms[]`에 남는다.
* 결과 순서는 보장하지 않는다.

### 응답 상태

| 상황 | HTTP | 본문 |
|---|---|---|
| 모든 공급사 성공 | 200 | `rooms[]`, `failures: []` |
| 일부 공급사 실패 | 200 | 성공한 공급사의 `rooms[]` + `failures[]` |
| 조건에 맞는 보유 객실이 없음 | 200 | `rooms: []`, `failures: []` |
| **모든 공급사 실패** | **502** | `error.errorCode = E2006`, `error.data` = 공급사별 실패 목록 |
| 파라미터 오류 | 400 | `E400`, `E1001`~`E1004` |

### 예시

**① 정상**

```bash
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

* 같은 "Riverside Hotel Seoul"이 A·B에서 각각 다른 `stayId`로 나온다 (병합하지 않음).
* "Namsan Garden Stay"는 둘째 날 잔여가 0이라 `availableRooms: 0`으로 남는다.

**② 부분 실패 — B 장애 (HTTP 200 + `resultCode: E503`)**

```json
{
  "resultType": "SUCCESS",
  "data": {
    "rooms": [ "… A의 상품 2건 …" ],
    "failures": [
      { "supplier": "B", "type": "UNAVAILABLE", "code": "E503" }
    ]
  },
  "error": null
}
```

**③ 전체 실패 — A 무응답 + B 장애** (HTTP 502, 약 2초 — A의 응답 타임아웃)

```json
{
  "resultType": "ERROR",
  "data": null,
  "error": {
    "errorCode": "E2006",
    "message": "모든 공급사 호출이 실패했습니다.",
    "data": [
      { "supplier": "A", "type": "UNAVAILABLE", "code": "TIMEOUT" },
      { "supplier": "B", "type": "UNAVAILABLE", "code": "E503" }
    ]
  }
}
```

**④ 파라미터 오류**

```json
// checkOut이 checkIn보다 앞섬 → 400
{ "resultType": "ERROR", "data": null,
  "error": { "errorCode": "E1001", "message": "체크인·체크아웃 날짜가 필요하며, 체크아웃은 체크인 이후여야 합니다.", "data": null } }

// adults 누락 → 400
{ "resultType": "ERROR", "data": null,
  "error": { "errorCode": "E400", "message": "요청 값이 올바르지 않습니다.", "data": { "adults": "성인 수는 필수입니다" } } }
```

---

## 2. 숙소 목록 동기화 (수동)

```
POST /api/v1/stays/sync
```

모든 공급사의 숙소 목록 API를 호출해 숙소·객실 타입 매핑을 갱신한다. 기동 시와 매일 04:00에 자동으로도 실행된다.

| 상황 | HTTP | 본문 |
|---|---|---|
| 실행 완료 | 201 | `{ "resultType": "SUCCESS", "data": null, "error": null }` |

* 공급사 한 곳의 목록 조회가 실패해도 **요청은 201**이다. 실패한 공급사는 기존 매핑을 유지하고 warn 로그만 남긴다 (공급사별 결과는 로그로 확인).
* 목록 조회는 `UNAVAILABLE`·`INTERNAL`이면 최대 2회 재시도한다 (2s → 6s).

---

## 3. Mock 공급사 제어

Mock(9090)은 공급사별로 모드를 바꿔 정상 / 장애 / 무응답을 재현한다. 모드는 숙소 목록과 재고·요금 API에 함께 적용되며, 재시작하면 `normal`로 돌아간다.

```
POST http://localhost:9090/control/{supplier}/mode?value={mode}
```

| `supplier` | `mode` | A의 동작 | B의 동작 |
|---|---|---|---|
| `a` / `b` | `normal` | 정상 응답 | 정상 응답 (`resultCode: 0000`) |
| `a` / `b` | `error` | HTTP 503 + `{"error":"SERVICE_UNAVAILABLE"}` | **HTTP 200** + `{"resultCode":"E503"}` |
| `a` / `b` | `no-response` | 응답하지 않음 (연결만 됨) | 응답하지 않음 |

```bash
# B 장애 → 부분 실패 확인
curl -X POST "http://localhost:9090/control/b/mode?value=error"
curl "http://localhost:8080/api/v1/stays/search?checkIn=2026-10-01&checkOut=2026-10-04&adults=2"

# A 무응답까지 → 502 전체 실패 확인 (약 2초)
curl -X POST "http://localhost:9090/control/a/mode?value=no-response"
curl "http://localhost:8080/api/v1/stays/search?checkIn=2026-10-01&checkOut=2026-10-04&adults=2"

# 원래대로
curl -X POST "http://localhost:9090/control/a/mode?value=normal"
curl -X POST "http://localhost:9090/control/b/mode?value=normal"
```

같은 공급사를 계속 실패시키면 최소 10회 호출 이후 서킷이 열려 `code: CIRCUIT_OPEN`으로 바로 실패한다 (10초 뒤 반열림).

---

## 4. 오류 코드

| errorCode | HTTP | 메시지 | 언제 |
|---|---|---|---|
| `E400` | 400 | 요청 값이 올바르지 않습니다. | 필수 파라미터 누락, 타입 불일치, `adults < 1` 등 Bean Validation 실패. `data`에 필드별 메시지 |
| `E1001` | 400 | 체크인·체크아웃 날짜가 필요하며, 체크아웃은 체크인 이후여야 합니다. | `checkOut <= checkIn` |
| `E1002` | 400 | 체크인은 오늘 이후여야 합니다. | 과거 체크인 |
| `E1003` | 400 | 숙박은 최대 30박까지 검색할 수 있습니다. | 30박 초과 |
| `E1004` | 400 | 성인은 1명 이상, 아동은 0명 이상이어야 합니다. | 인원 규칙 위반 |
| `E2006` | 502 | 모든 공급사 호출이 실패했습니다. | 검색 대상 공급사 전부 실패. `data`에 공급사별 실패 |
| `E500` | 500 | 알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요. | 처리되지 않은 예외 |

### 공급사 실패 분류 (`failures[].type`)

| type | 원본 코드 예 | 의미 |
|---|---|---|
| `UNAVAILABLE` | `SERVICE_UNAVAILABLE`, `E503`, `TIMEOUT`, `CONNECT_REFUSED`, `CONNECT_TIMEOUT`, `DNS`, `POOL_EXHAUSTED`, `CIRCUIT_OPEN`, `TOTAL_TIMEOUT_EXCEEDED` | 공급사에 닿지 않거나 일시 장애 |
| `INTERNAL` | A의 5xx(503 제외), `E500`, B의 알 수 없는 `resultCode` | 공급사 내부 오류 |
| `AUTH` | `UNAUTHORIZED`, `E401` | 인증 실패 |
| `RATE_LIMITED` | `RATE_LIMIT_EXCEEDED`, `E429` | 호출 한도 초과 |
| `BAD_REQUEST` | A의 4xx, `E400` | 우리 요청이 잘못됨 |
| `MALFORMED` | `UNPARSEABLE`, `EMPTY_BODY`, `NULL_DATA`, `BODY_TOO_LARGE`, `UNEXPECTED` | 응답을 해석할 수 없음 |

분류 기준과 재시도·서킷·묶음 중단과의 관계: [README #16](../README.md#16-a의-4xx5xx-b의-resultcode-타임아웃-연결-실패를-하나의-분류로-묶은-이유는-분류-축을-무엇으로-잡았는가)
