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

## 코드 범위

| 범위 | 대상 |
|------|------|
| `E400`, `E409`, `E429`, `E500` | 공통 |
| `E1000`~ | 도메인별. 도메인 패키지가 확정되면 범위를 이 표에 추가한다 |

## 새 에러 추가 절차

1. `ErrorCode`에서 해당 도메인 범위의 다음 번호를 확인한다.
2. `ErrorCode` enum에 코드를 추가한다.
3. `ErrorType` enum에 상태, 코드, 메시지, LogLevel을 추가한다.

LogLevel 선택:
- `INFO`: 정상 흐름의 일부
- `WARN`: 클라이언트 입력 오류, 비즈니스 규칙 위반
- `ERROR`: 예상하지 못한 시스템 오류
