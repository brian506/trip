# ErrorType / ErrorCode 패턴

## 구조

`ErrorCode`(코드 식별자 enum)와 `ErrorType`(HttpStatus, 코드, 메시지, LogLevel을 담은 enum)으로 나눈다.

```java
// ErrorType 추가 예시
INVALID_STAY_PERIOD(HttpStatus.BAD_REQUEST, ErrorCode.E1000, "체크아웃은 체크인 이후여야 합니다.", LogLevel.WARN),

// throw
throw new AppException(ErrorType.INVALID_STAY_PERIOD);
```

## 응답 형식

```json
{ "resultType": "SUCCESS", "data": { ... }, "error": null }
{ "resultType": "ERROR", "data": null, "error": { "errorCode": "E400", "message": "...", "data": null } }
```

`error.data`에는 구조체를 담아도 된다. 검증 실패는 `{필드: 사유}` Map을, 공급사 전체 실패(`SUPPLIER_ALL_FAILED`)는 `List<SupplierFailure>`를 담는다.

## 코드 범위

| 범위 | 대상 |
|------|------|
| `E400`, `E409`, `E429`, `E500` | 공통 |
| `E1000`~`E1999` | `stay` 도메인 |
| `E2000`~`E2999` | 공급사 연동(`supplier`) |

## 새 에러 추가 절차

1. `ErrorCode`에서 해당 도메인 범위의 다음 번호를 확인한다.
2. `ErrorCode` enum에 코드를 추가한다.
3. `ErrorType` enum에 상태, 코드, 메시지, LogLevel을 추가한다.

LogLevel 선택:
- `INFO`: 정상 흐름의 일부
- `WARN`: 클라이언트 입력 오류, 비즈니스 규칙 위반
- `ERROR`: 예상하지 못한 시스템 오류
