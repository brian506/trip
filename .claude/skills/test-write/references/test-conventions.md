# 테스트 작성 규칙

`/test-write`가 테스트를 쓸 때 따르는 구조와 형식이다. 무엇을 테스트할지는 `SKILL.md`의 절차(사용자 선택)가 정한다.

## 종류와 위치

**`{도메인}/{계층}/*Test` 두 단계다.** `src/main`의 패키지를 그대로 베끼지 않는다 —
공급사별 `a/`·`b/` 갈래는 테스트에서 펴서 계층 하나로 둔다(`supplier/a/response` → `supplier/response`).
세 단계부터는 파일 두 개짜리 디렉터리가 늘어 찾기만 어려워진다.

| 종류 | 대상 | 방식 | 위치 | 실행 |
|------|------|------|------|------|
| 도메인 단위 | VO | 순수 JUnit | `{domain}/vo/*Test` | `test` |
| 컴포넌트 단위 | Manager 등 implement | Mockito | `{domain}/implement/*Test` | `test` |
| 서비스 단위 | business | Mockito | `{domain}/business/*ServiceTest` | `test` |
| 공급사 응답 정규화 | `A*`·`B*` 응답 record의 `toSupplier*` | 순수 JUnit | `supplier/response/*Test` | `test` |
| 공급사 입력 모델 단위 | `SupplierStayCodes` 등 `supplier`의 VO | 순수 JUnit | `supplier/vo/*Test` | `test` |
| 공급사 코드 매핑 | `BResultCode` 등 계층이 없는 것 | 순수 JUnit | `supplier/*Test` | `test` |
| 공급사 Client 단위 | WebClient 호출, 타임아웃, 에러 응답 | MockWebServer | `supplier/*ClientTest` | `test` |
| 통합 | 서비스 + H2 | `SpringTest` 상속 | `{domain}/integration/*IntegrationTest` | `integrationTest` |

- 픽스처는 `{domain}/fixture/*Fixture`에 둔다. static 상수와 팩토리 메서드, 또는 enum 픽스처로 직접 만든다.
- 공통 지원 클래스는 테스트 소스의 `com.trip.support`에 둔다: `IntegrationTest`, `SpringTest`.

## 단위 테스트

- `@ExtendWith(MockitoExtension.class)`와 `@Mock`을 쓴다.
- 대상은 `@InjectMocks` 없이 `@BeforeEach`에서 생성자로 조립한다. 하위 implement는 실제 객체로 조립하고, 가장 바깥 의존(Repository, 외부 Client)만 mock으로 둔다.
- 스터빙은 BDDMockito로 한다: `given(..).willReturn(..)`. `when(..)`은 쓰지 않는다.
- 단언은 AssertJ만 쓴다: `assertThat`, `assertThatThrownBy`.

### 검증 범위 — 반환값과 예외만

`then(..).should()`·`verify(..)`로 **호출을 검증하지 않는다.** 대상이 무엇을 돌려주는지와 무엇을 던지는지만 단언한다.
mock은 상황을 만들기 위한 입력이지 검증 대상이 아니다.

이유는 호출 검증이 구현의 협력 구조를 계약으로 굳히기 때문이다. 같은 결과를 내는 다른 조립으로 바꾸면
동작이 멀쩡한데 테스트가 깨지고, 반대로 결과가 틀려도 호출만 맞으면 통과한다.

그래서 **반환값이 없는 메서드는 단위 테스트로 쓰지 않는다.** 저장·동기화처럼 부수효과만 있는 경로는
통합 테스트에서 DB를 다시 읽어 상태로 확인한다(아래 통합 테스트).

MockWebServer의 `takeRequest()`로 **실제로 나간 HTTP 요청**을 단언하는 것은 여기 해당하지 않는다.
mock 상호작용이 아니라 공급사가 받는 계약이라 그대로 쓴다.

## 통합 테스트

- `SpringTest`를 상속한다. `@IntegrationTest` + `@ActiveProfiles("test")` + `@Transactional`이라 테스트마다 롤백된다.
- DB는 `application-test.yaml`의 H2 인메모리이고, 컨텍스트마다 이름이 달라 서로 섞이지 않는다.
- 별도 스레드나 Reactor에서 커밋한 데이터는 롤백되지 않는다. 그런 테스트는 끝날 때 직접 정리한다.
- 공급사 호출은 MockWebServer로 대체한다. 9090 Mock 서버나 실제 외부 주소를 호출하지 않는다.

## 공급사 Client (MockWebServer)

- `mockwebserver3.MockWebServer`를 테스트마다 시작·종료하고, Client의 base URL을 서버 주소로 지정한다.
- 지연, 타임아웃, 5xx, 잘못된 본문은 응답 설정으로 실제로 재현한다.
- 보낸 요청은 `takeRequest()`로 꺼내 경로, 쿼리, 헤더(`X-Api-Key`)를 단언한다.

## 형식

- `@DisplayName`은 한글 문장이다. 명세이므로 테스트에 맞춰 바꾸지 않는다.
- 메서드명은 영어 camelCase로 문장을 요약한다. 예: `throwAppExceptionIfCheckOutIsBeforeCheckIn`
- 본문은 `// given`, `// when`, `// then` 주석으로 나눈다.
