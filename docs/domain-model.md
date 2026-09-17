# 통합 모델 설계

공급사 A·B가 서로 다르게 표현하는 숙박 상품을 하나의 모델로 모은 방식. 결정의 근거는 [README 설계 의사결정 1~8, 12](../README.md#설계-의사결정)에 있다.

## 1. 단위

| 단위 | 성격 | 원본 | 저장 |
|---|---|---|---|
| 숙소 | 거의 안 바뀜 | 공급사 숙소 목록 API | **DB** — 코드 ↔ 내부 ID 매핑 |
| 객실 타입 | 거의 안 바뀜 | 공급사 숙소 목록 API | **DB** — 코드 ↔ 내부 ID 매핑 |
| 요금 | 매번 바뀜 | 공급사 재고·요금 API | 저장 안 함, 검색마다 조회 |
| 재고 | 매번 바뀜 | 공급사 재고·요금 API | 저장 안 함, 검색마다 조회 |

재고·요금 API는 숙소 코드 목록을 받아야 조회되므로, **어떤 숙소를 물어볼지 알기 위해** 숙소·객실 매핑을 미리 저장해 둔다.

## 2. 파이프라인과 VO

```
공급사 응답 DTO                 공급사 경계 VO            도메인 VO
─────────────────              ────────────────          ──────────────
AHotel / BProperty        →    SupplierStay         →    Stay (엔티티)
ARoomType / BRoom         →    SupplierRoomType     →    RoomType (엔티티)
                                                          ↓ 검색 시
                                                          RoomOption   (DB 후보: 내부 ID + 공급사 코드)
AAvailabilityItem         →    SupplierRoom         ─┐
BSearchItem               →    SupplierRoom         ─┴→  SearchedRoom (응답)
```

* `SupplierRoom`까지는 공급사 코드(`stayCode`, `roomTypeCode`)를 들고 있다.
* `RoomOption`과 `SupplierRoom`을 `(supplier, stayCode, roomTypeCode)`로 결합한 뒤, 응답(`SearchedRoom`)에는 **내부 ID만** 남긴다.
* 숙소명·객실명·최대 인원은 재고 응답이 아니라 **DB 매핑(목록 API 기준)** 값을 쓴다. 두 API가 다른 이름을 주더라도 결과가 한 기준으로 나간다.

## 3. 검색 결과 모델 — `SearchedRoom`

| 필드 | 타입 | 의미 | 출처 |
|---|---|---|---|
| `stayId` | UUID | 내부 숙소 ID | DB `stay.stay_id` |
| `stayName` | string | 숙소명 | DB `stay.name` |
| `roomTypeId` | UUID | 내부 객실 타입 ID | DB `room_type.room_type_id` |
| `roomTypeName` | string | 객실 타입명 | DB `room_type.name` |
| `maxOccupancy` | int | 객실 1실 최대 인원 | DB `room_type.max_occupancy` |
| `availableRooms` | int | 요청 기간 전체를 예약할 수 있는 객실 수. **0이면 예약 불가** | 공급사 |
| `supplier` | `A` \| `B` | 출처 공급사 | 공급사 |
| `breakfastIncluded` | boolean | 조식 포함 여부 (값이 없으면 false) | 공급사 |
| `currency` | string | 통화 (ISO 4217). 변환하지 않고 공급사 값을 그대로 전달 | 공급사 |
| `totalPrice` | long | **숙박 기간 전체, 세금 포함 총액** | 공급사 |

## 4. 공급사 필드 매핑

### 4.1 숙소 목록 → 매핑

| 표준 | A (`GET /a/v1/hotels`) | B (`GET /b/api/properties`) |
|---|---|---|
| 숙소 코드 | `items[].hotelCode` | `data.items[].propertyId` |
| 숙소명 | `items[].hotelName` | `data.items[].propertyName` |
| 객실 타입 코드 | `items[].roomTypes[].roomTypeCode` | `data.items[].rooms[].roomId` |
| 객실 타입명 | `items[].roomTypes[].roomTypeName` | `data.items[].rooms[].roomName` |
| 최대 인원 | `items[].roomTypes[].maxOccupancy` | `data.items[].rooms[].maxOccupancy` |

검증 (`SupplierStay`, `SupplierRoomType` 생성자): 코드·이름이 비었거나 최대 인원이 1 미만이면 그 공급사의 이번 반영 전체를 건너뛴다 ([README #19](../README.md#19-목록-항목-하나가-검증에-실패했을-때-그-항목만-빼지-않고-공급사-전체를-건너뛰는-이유는)).

### 4.2 재고·요금 → `SupplierRoom`

| 표준 | A (`GET /a/v1/availability`) | B (`GET /b/api/search`) |
|---|---|---|
| 숙소 코드 | `hotelCode` | `propertyId` |
| 객실 타입 코드 | `roomTypeCode` | `roomId` |
| `totalPrice` | 기간 내 날짜별 `dailyRates[].nightlyRate + taxAmount`의 **합** | `totalPrice` 그대로 (세금 포함) |
| `availableRooms` | 기간 내 날짜별 `dailyRates[].remainingRooms`의 **최솟값** | 기간 내 날짜별 `inventory[].remainingRooms`의 **최솟값** |
| `breakfastIncluded` | `breakfastIncluded` | `breakfastIncluded` |
| `currency` | `currency` | `currency` |

**버리는 필드**

| 필드 | 이유 |
|---|---|
| A `dailyRates[].nightlyRate`, `taxAmount` (날짜별 값 자체) | B에는 없어 표준에 올리면 B는 추정값이 됨 — 총액 계산에만 씀 ([#3](../README.md#3-a만-주는-날짜별-단가세액을-살리지-않고-버린-이유는)) |
| B `taxIncluded` | 기준을 세금 포함으로 통일해서 불필요 ([#2](../README.md#2-기준을-세금-포함-총액으로-잡은-이유는)) |
| 재고 응답의 숙소명·객실명·최대 인원 | DB 매핑 값을 기준으로 씀 |

**항목을 결과에서 빼는 경우**

* 요청 기간(체크인 ~ 체크아웃 전날) 중 **하루라도** 날짜 항목이 없음
* A: 그날의 `remainingRooms`, `nightlyRate`, `taxAmount` 중 하나라도 없음
* B: `totalPrice` 또는 `inventory`가 없음, 또는 그날의 `remainingRooms`가 없음
* DB 후보(`RoomOption`)에 없는 `(공급사, 숙소 코드, 객실 코드)` 조합

일부 날짜만으로 계산한 총액·재고는 실제보다 싸거나 많아 보이므로, 불완전한 항목은 내보내지 않는다.

### 4.3 연박 재고 판정 예시

3박(10/01~10/03) 요청, 날짜별 잔여 `[2, 0, 4]` → `availableRooms = 0` → 예약 불가지만 응답에는 남긴다 ([#8](../README.md#8-연박-검색에서-예약-가능-객실-수를-어떻게-판정하는가-0인-상품은-응답에서-빼는가)).

## 5. 식별자

| 대상 | 내부 ID | 공급사 쪽 유일 키 | DB 제약 |
|---|---|---|---|
| 숙소 | `stay_id` UUID (앱 생성) | (공급사, 숙소 코드) | `UNIQUE (supplier, stay_code)` |
| 객실 타입 | `room_type_id` UUID (앱 생성) | (숙소, 객실 코드) | `UNIQUE (stay_id, room_type_code)` |

* 같은 공급사 상품은 동기화를 반복해도 유일 키로 기존 행을 찾아 **항상 같은 내부 ID**로 돌아온다.
* 공급사가 다르면 실제로 같은 숙소여도 **다른 내부 ID**다 (병합하지 않음, [#12](../README.md#12-a와-b의-같은-숙소를-합치지-않고-따로-노출한-이유는)).
* 목록에서 사라진 숙소·객실은 삭제하지 않고 `active=false`로 둔다. 다시 나타나면 같은 행을 재활성화하므로 ID가 유지된다.

## 6. DB 스키마

원본: `src/main/resources/schema.sql` (JPA는 `validate`만)

```
stay                                    room_type
─────────────────────────────           ─────────────────────────────
stay_id      UUID  PK                   room_type_id    UUID  PK
supplier     VARCHAR(16)                stay_id         UUID  FK → stay
stay_code    VARCHAR(64)                room_type_code  VARCHAR(64)
name         VARCHAR(255)               name            VARCHAR(255)
active       BOOLEAN                    max_occupancy   INT
created_at   TIMESTAMP                  active          BOOLEAN
updated_at   TIMESTAMP                  created_at      TIMESTAMP
                                        updated_at      TIMESTAMP
UNIQUE (supplier, stay_code)            UNIQUE (stay_id, room_type_code)
INDEX  (supplier, active)               INDEX  (stay_id)
```

JPA 연관관계(`@ManyToOne`)는 두지 않고 FK 제약으로만 관계를 표현한다 ([#6](../README.md#6-유일-키를-공급사-숙소-코드숙소-객실-코드로-잡은-이유는-jpa-연관관계-없이-fk-제약만-둔-이유는)).

## 7. 모델이 다루지 않는 것

| 항목 | 현재 |
|---|---|
| 통화 변환 | `currency`를 그대로 전달하고 환산하지 않음. 통화가 다른 상품끼리는 `totalPrice`를 바로 비교할 수 없음 (Mock 데이터는 모두 KRW) |
| 날짜별 요금 | 응답에 없음 |
| 취소 규정·요금제(플랜) | 공급사 스펙에 없어 모델에 없음 |
| 중복 숙소 병합 | 하지 않음 |
| 요금·재고 캐시 | 없음. 매 검색마다 공급사 호출 (설계 방향은 [README #23](../README.md#23-요금재고를-캐시한다면-어디까지-얼마나-오래-둘-것인가)) |
