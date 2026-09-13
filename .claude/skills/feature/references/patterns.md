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
- 도메인 제약 검증은 Request가 아니라 VO에서 한다. Request는 VO로 변환하는 역할만 한다.
- Bean Validation 어노테이션은 VO로 표현하지 않는 요청 형식 검증에만 쓴다.

## Command 사용 판단

| 상황 | 방식 |
|------|------|
| 조회 파라미터 1~2개 | 직접 파라미터 |
| 단일 값 형식 검증이 필요한 경우 | VO를 직접 전달 |
| 필드 3개 이하, 단순 전달 | Request 필드를 직접 전달 |
| 필드 4개 이상이거나 VO 여러 개로 변환해야 하는 경우 | Command + `toCommand()` |

```java
// Controller
stayService.search(request.toCommand());

// Request — toCommand() 안에서 VO를 생성하고, VO 생성자가 검증한다
public SearchCommand toCommand() {
    return new SearchCommand(new StayPeriod(checkIn, checkOut), new Guests(adults, children));
}
```

## business 패키지에 전달용 DTO를 두지 않는다

계층 간 흐름 객체는 `vo/`에 둔다. `business/`에는 Service와 Command만 둔다.

## ApiResponse 반환

- 데이터 있음: `ResponseEntity.ok(ApiResponse.success(response))`
- 데이터 없음: `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success())`
- 에러 응답은 `ApiControllerAdvice`가 `ApiResponse.error(...)`로 만든다. Controller에서 직접 만들지 않는다.

## Swagger

- Controller 클래스에 `@Tag`, 메서드에 `@Operation(summary, description)`을 쓴다.
- 부분 실패처럼 응답에서 해석이 필요한 부분은 `description`에 적는다.
