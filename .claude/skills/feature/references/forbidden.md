# 코드 금지 규칙

## Decision

- **`.claude/references/decisions.md`의 미결정 항목을 임의로 도입하면 안 된다**
  - 의존성, 설정, 패턴 모두 해당한다. 필요하면 선택지와 트레이드오프를 정리해 묻는다
- **확장을 대비한 추상화나 클래스 분리를 미리 만들면 안 된다**

## Architecture

- **Business가 Repository를 import하거나 주입받으면 안 된다**
- **Controller가 Implement를 호출하면 안 된다**
- **Implement가 다른 도메인의 Business를 참조하면 안 된다**
- **JPA 엔티티를 메서드 파라미터로 계층 간 전달하면 안 된다** (VO의 `from(Entity)`는 예외)
- **`business/`에 Service·Command 외의 전달용 타입을 두면 안 된다**

## Supplier Integration

- **공급사 고유 요청·응답 타입이 Implement 밖으로 나가면 안 된다**
- **Business·Controller가 공급사 종류에 따라 분기하면 안 된다**
- **애플리케이션 코드가 `supplier` 패키지(Mock 서버)를 import하면 안 된다**
- **`supplier` 패키지의 빈에서 `@Profile("supplier")`를 빠뜨리면 안 된다**

## Implement

- **CRUD를 Reader·Writer로 나누거나 별도 Validator를 만들면 안 된다**
- **VO가 검증하는 제약을 Service·Manager·Request에서 다시 검사하면 안 된다**

## Response

- **Controller가 `ApiResponse`로 감싸지 않고 반환하면 안 된다**
- **`@ApiResponses`를 쓰면 안 된다**

## Code Structure

- **클래스 안에 중첩 타입을 선언하면 안 된다**
