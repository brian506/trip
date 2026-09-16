# 구현 패턴

## VO 검증

도메인 객체가 가져야 할 제약은 VO 안에서 검증한다. 검증이 흩어지지 않도록 다른 곳에 같은 검사를 두지 않는다.

- `record`를 쓰고, compact constructor에서 검증한다.
- 검증 대상: null, 범위, 형식, 필드 간 관계(예: 체크아웃은 체크인 이후)
- 실패하면 `AppException(ErrorType)`을 던진다. 필요한 ErrorType은 `error-codes.md` 절차로 추가한다.
- 검증에서 파생되는 계산(예: 숙박일 수)도 해당 VO의 메서드로 둔다.

```java
public record Guests(Integer adults, Integer children) {

    public Guests {
        if (adults == null || children == null) {
            throw new AppException(ErrorType.GUESTS_IS_NULL);
        }
        if (adults < 1 || children < 0) {
            throw new AppException(ErrorType.INVALID_GUESTS);
        }
    }

    public int total() {
        return adults + children;
    }
}
```

위 코드는 형태 예시다. 실제 VO와 ErrorType은 설계 단계에서 정한다.

## Request 패턴

- `record`를 쓴다.
- 필드 하나만 보면 판단되는 제약(null, 범위)은 Request에 Bean Validation으로 선언하고 Controller에서 `@Valid`를 건다.
- 필드 간 관계(예: 체크아웃은 체크인 이후)와 도메인 불변식은 VO 생성자가 검증한다. 이를 위해 클래스 레벨 커스텀 제약이나 별도 Validator를 만들지 않는다.
- 같은 제약이 Request와 VO 양쪽에 있는 것은 중복으로 보지 않는다. 지키는 대상이 다르다.
  - Request: HTTP 계약. 오류를 필드별로 한 번에 돌려주고 OpenAPI 스키마에 제약이 드러난다.
  - VO: 객체 불변식. HTTP 밖 경로(스케줄러, 테스트)에서 만들어도 잘못된 상태가 생기지 않는다.
- 검증 실패 응답은 `ApiControllerAdvice`가 `INVALID_ACCESS_PATH`(E400)에 `{필드: 사유}`를 담아 내려준다. 타입 변환 실패는 내부 타입이 새지 않도록 일반 메시지로 바꾼다.

## Service 입력 판단

| 상황 | 방식 |
|------|------|
| 조회 파라미터 1~2개 | 직접 파라미터 |
| 단일 값 형식 검증이 필요한 경우 | VO를 직접 전달 |
| 필드 3개 이하, 단순 전달 | Request 필드를 직접 전달 |
| VO 여러 개로 변환해야 하는 경우 | VO들을 그대로 파라미터로 전달 |
| 파라미터가 늘어 시그니처가 길어질 때 | `vo/`에 조건 VO를 만들어 묶는다 |

입력을 한 번 더 감싸는 Command record를 `business/`에 두지 않는다. 필드를 옮겨 담기만 하는 타입은 파라미터 목록의 별칭일 뿐이고, 출력(`vo/`)과 입력(`business/`)이 서로 다른 패키지·이름 규칙을 따르게 된다.

```java
// Controller
stayService.search(request.toPeriod(), request.toGuests());

// Request — VO를 생성하고, VO 생성자가 검증한다
public StayPeriod toPeriod() {
    return new StayPeriod(checkIn, checkOut);
}

public Guests toGuests() {
    return new Guests(adults, children);
}
```

## business 패키지에 전달용 DTO를 두지 않는다

계층 간 흐름 객체는 `vo/`에 둔다. `business/`에는 Service만 둔다.

## ApiResponse 반환

- 데이터 있음: `ResponseEntity.ok(ApiResponse.success(response))`
- 데이터 없음: `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success())`
- 에러 응답은 `ApiControllerAdvice`가 `ApiResponse.error(...)`로 만든다. Controller에서 직접 만들지 않는다.
- 응답 body는 VO를 그대로 `ApiResponse.success(...)`에 넣는다. `controller/response` record는 응답 모양이 VO와 다를 때만 만든다(내부 enum을 외부 문자열로 번역, 파생 필드, `@Schema` 문서화). 필드가 VO와 같고 `from`이 값을 그대로 옮기기만 하는 record는 두지 않는다.

## Swagger

- Controller 클래스에 `@Tag`, 메서드에 `@Operation(summary, description)`을 쓴다.
- 부분 실패처럼 응답에서 해석이 필요한 부분은 `description`에 적는다.
