# 아키텍처 규칙

API 수가 적은 소규모 프로젝트다. 클래스를 역할별로 잘게 쪼개기보다 추적하기 쉬운 구조를 우선한다.

## 계층 구조 (순방향 참조만 허용)

```
Controller → Business(Service) → Implement → DataAccess
```

- 역방향 참조와 계층 건너뛰기를 금지한다.
- Business는 Repository를 직접 참조하지 않는다.
- Implement 계층 안의 협력은 허용한다.
- 도메인의 모든 계층은 `external` 패키지를 참조할 수 있다. `external`은 도메인 패키지를 참조하지 않는다.

## 패키지 구조

```
com.trip/
├── common/controller/   # ApiControllerAdvice
├── config/              # Swagger 등 공통 설정
├── support/             # ApiResponse, AppException, ErrorType, ErrorCode
├── external/supplier/   # 외부 공급사 연동 (인터페이스·구현·설정·입력 모델 전부)
│   ├── a/  b/           # 공급사별 Client, 코드 매핑, dto/
├── mock/supplier/       # 공급사 Mock 서버 (supplier 프로파일 전용)
└── {domain}/            # 현재 stay
    ├── controller/      # REST Controller
    │   ├── request/
    │   └── response/
    ├── business/        # Service — 유스케이스 오케스트레이션
    ├── implement/       # Manager
    ├── dataaccess/      # Entity, Repository
    │   ├── entity/
    │   └── repository/
    └── vo/              # 도메인 객체 (불변 record) — 검증 포함
```

- 도메인에 해당 계층이 필요 없으면 그 패키지를 만들지 않는다.

## Implement 계층 역할

| 역할 | 사용 시점 |
|------|----------|
| **Manager** | 한 저장소에 대한 조회·저장·수정·삭제 전부. 트랜잭션 경계 |

- CRUD는 저장소 단위로 Manager 클래스 하나에 모은다. Reader, Writer로 나누지 않는다.
- 별도 Validator 클래스를 만들지 않는다. 값 하나의 제약은 VO가 검증하고, 저장소 조회가 필요한 규칙(중복 등)은 Manager가 검증한다.
- 한 Manager가 지나치게 커지거나 역할을 더 나눠야 할 이유가 생기면 사용자에게 먼저 묻는다.

## 외부 공급사 연동 경계

- 외부 공급사 연동 코드는 `external/supplier`에 모은다: `SupplierClient` 인터페이스, `SupplierHttpCaller`·`SupplierErrors`, `SupplierCallException`·`SupplierFailureType`, 설정(`WebClientConfig`·`SupplierProperties`), 입력 모델(`Supplier`·`SupplierStay`·`SupplierRoomType`).
- 공급사별 코드는 하위 패키지(`a/`, `b/`)에 둔다: Client, 공급사 코드 → 내부 분류 매핑(예: `BResultCode`), `dto/`.
- 공급사 고유의 요청·응답 형식과 코드는 공급사 하위 패키지 밖으로 나가지 않는다. Client에서 `external/supplier`의 표준 모델로 변환해 반환한다.
- Business와 Controller는 공급사 종류에 따라 분기하지 않는다. 공급사별 차이는 해당 Client 안에서 흡수한다.

## mock 패키지 (공급사 Mock 서버)

- 외부 공급사를 흉내 내는 Mock 서버 코드는 `mock/supplier` 패키지에만 둔다.
- `mock` 패키지의 모든 빈에 `@Profile("supplier")`를 붙인다. 기본 프로파일로 뜬 애플리케이션에 등록되면 안 된다.
- 애플리케이션 코드는 `mock` 패키지를 import하지 않는다. 연동은 HTTP로만 한다.
- `mock` 패키지는 계층 규칙과 코드 품질 규칙의 적용 대상이 아니다.

## VO

- `vo/`는 도메인 개념을 담는 불변 record다. 계층 간 데이터 전달에 쓴다.
- 도메인 객체가 지켜야 할 제약(null 금지, 범위, 형식, 필드 간 관계)은 VO 생성자에서 검증한다. 상세는 `patterns.md`.
- entity → VO 변환은 VO의 `from()` 팩토리 메서드가 맡고, Manager가 변환해서 반환한다.

## 중첩 타입 금지

클래스·record·interface 안에 다른 클래스·record·enum·interface를 선언하지 않는다. 모든 타입은 독립 파일로 분리한다.
