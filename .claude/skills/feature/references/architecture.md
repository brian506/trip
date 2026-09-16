# 아키텍처 규칙

API 수가 적은 소규모 프로젝트다. 클래스를 역할별로 잘게 쪼개기보다 추적하기 쉬운 구조를 우선한다.

## 계층 구조 (순방향 참조만 허용)

```
Controller → Business(Service) → Implement → DataAccess
```

- 역방향 참조와 계층 건너뛰기를 금지한다.
- Business는 Repository를 직접 참조하지 않는다.
- Implement 계층 안의 협력은 허용한다.
- 도메인의 모든 계층은 `supplier` 패키지를 참조할 수 있다. `supplier`는 도메인 패키지를 참조하지 않는다.

## 패키지 구조

```
com.trip/
├── common/controller/   # ApiControllerAdvice
├── config/              # 공통 설정
│   ├── swagger/         # SwaggerConfig
│   └── supplier/        # WebClientConfig (공급사별 WebClient 빈)
├── support/             # ApiResponse, AppException, ErrorType, ErrorCode
├── supplier/            # 외부 공급사 연동 — 루트에는 계약(SupplierClient, Supplier)만
│   ├── vo/              # 도메인에 넘기는 모델 (SupplierStay, SupplierRoomType, SupplierFailure)
│   ├── exception/       # SupplierCallException, SupplierFailureType, SupplierErrors
│   ├── infra/           # SupplierHttpCaller, SupplierProperties, SupplierEndpoint
│   ├── a/  b/           # 공급사별 Client, 코드 매핑, response/
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
- `controller/response`는 응답 모양이 VO와 다를 때만 만든다. 필드가 VO와 똑같고 `from`이 값을 그대로 옮기기만 하는 record는 두지 않고, VO를 그대로 `ApiResponse.success()`에 넣는다. 내부 enum을 외부 문자열로 번역하거나 파생 필드를 두거나 `@Schema`를 붙일 일이 생기면 그때 만든다. 현재 `stay`에는 `controller/request`만 있다.

## Implement 계층 역할

| 역할 | 사용 시점 |
|------|----------|
| **Manager** | 한 저장소에 대한 조회·저장·수정·삭제 전부. 트랜잭션 경계 |

- CRUD는 저장소 단위로 Manager 클래스 하나에 모은다. Reader, Writer로 나누지 않는다.
- 별도 Validator 클래스를 만들지 않는다. 값 하나의 제약은 VO가 검증하고, 저장소 조회가 필요한 규칙(중복 등)은 Manager가 검증한다.
- 한 Manager가 지나치게 커지거나 역할을 더 나눠야 할 이유가 생기면 사용자에게 먼저 묻는다.

## 외부 공급사 연동 경계

- 외부 공급사 연동 코드는 `supplier`에 모으고 역할별로 나눈다: 계약(`SupplierClient`·`Supplier`)은 루트, 입력 모델은 `vo/`, 실패 표현은 `exception/`, 실행 인프라는 `infra/`(`SupplierHttpCaller`·`SupplierProperties`·`SupplierEndpoint`).
- 공급사 WebClient 빈 등록(`WebClientConfig`)만 `config/supplier`에 둔다. 설정 클래스는 `config`에 모은다.
- 공급사별 코드는 하위 패키지(`a/`, `b/`)에 둔다: Client, 공급사 코드 → 내부 분류 매핑(예: `BResultCode`), 응답 형식은 `response/`.
- 공급사 응답 형식 → VO 변환은 각 응답 record의 `toSupplierStay()`·`toSupplierRoomType()`가 맡는다. Client는 요청 조립과 응답 판정만 한다.
- 공급사 호출 실패는 `SupplierCallException`(`AppException` 하위)으로 던진다. `SupplierFailureType`이 대응 `ErrorType`(`E2000`~)을 들고 있어 `ApiControllerAdvice`가 그대로 처리한다.
- 예외를 값으로 바꿔야 하는 경로(검색의 부분 실패)는 `SupplierCallException.toFailure()`를 쓴다. 같은 세 필드(supplier, type, code)를 도메인에서 다시 선언하지 않는다.
- 공급사 고유의 요청·응답 형식과 코드는 공급사 하위 패키지 밖으로 나가지 않는다. Client에서 `supplier`의 표준 모델로 변환해 반환한다.
- Business와 Controller는 공급사 종류에 따라 분기하지 않는다. 공급사별 차이는 해당 Client 안에서 흡수한다.

## mock-supplier 모듈 (공급사 Mock 서버)

- 외부 공급사를 흉내 내는 Mock 서버 코드는 별도 Gradle 모듈 `mock-supplier`에만 둔다. 루트 앱 소스(`src/main/java`)에 두지 않는다.

```
mock-supplier/
├── build.gradle                                  # webmvc 스타터만
└── src/main/
    ├── java/com/trip/mock/supplier/              # MockSupplierApplication, MockSupplierController
    └── resources/application.yaml                # server.port: 9090
```

- 루트 앱은 `mock-supplier`를 의존성으로 추가하지 않고, import도 하지 않는다. 연동은 HTTP로만 한다(`supplier.endpoints.*.base-url`).
- Mock은 자체 `@SpringBootApplication`으로 뜨는 독립 프로세스다. 프로파일로 빈을 거르지 않으므로 `@Profile`을 붙이지 않는다.
- Mock에 필요한 의존성은 `mock-supplier/build.gradle`에만 추가한다. 루트 `build.gradle`에 넣지 않는다.
- `mock-supplier` 모듈은 계층 규칙과 코드 품질 규칙의 적용 대상이 아니다.

## VO

- `vo/`는 도메인 개념을 담는 불변 record다. 계층 간 데이터 전달에 쓴다.
- 도메인 객체가 지켜야 할 제약(null 금지, 범위, 형식, 필드 간 관계)은 VO 생성자에서 검증한다. 상세는 `patterns.md`.
- entity → VO 변환은 VO의 `from()` 팩토리 메서드가 맡고, Manager가 변환해서 반환한다.

## 중첩 타입 금지

클래스·record·interface 안에 다른 클래스·record·enum·interface를 선언하지 않는다. 모든 타입은 독립 파일로 분리한다.
