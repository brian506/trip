# 테스트 작성 규칙

`/test-write`가 테스트를 쓸 때 따르는 구조와 형식이다. 무엇을 테스트할지는 `SKILL.md`의 절차(사용자 선택)가 정한다.

## 종류와 위치

테스트 패키지는 `src/main`의 패키지를 그대로 따른다.

| 종류 | 대상 | 방식 | 위치 | 실행 |
|------|------|------|------|------|
| 도메인 단위 | VO | 순수 JUnit | `{domain}/vo/*Test` | `test` |
| 컴포넌트 단위 | Manager 등 implement | Mockito | `{domain}/implement/*Test` | `test` |
| 서비스 단위 | business | Mockito | `{domain}/business/*ServiceTest` | `test` |
| 공급사 입력 모델 단위 | `SupplierStay` 등 `external/supplier`의 VO | 순수 JUnit | `external/supplier/*Test` | `test` |
| 공급사 Client 단위 | WebClient 호출, 타임아웃, 에러 응답 | MockWebServer | `external/supplier/{a,b}/*ClientTest` | `test` |
| 통합 | 서비스 + H2 | `SpringTest` 상속 | `{domain}/integration/*IntegrationTest` | `integrationTest` |

- 픽스처는 `{domain}/fixture/*Fixture`에 둔다. static 상수와 팩토리 메서드, 또는 enum 픽스처로 직접 만든다.
- 공통 지원 클래스는 테스트 소스의 `com.trip.support`에 둔다: `IntegrationTest`, `SpringTest`.

## 단위 테스트

- `@ExtendWith(MockitoExtension.class)`와 `@Mock`을 쓴다.
- 대상은 `@InjectMocks` 없이 `@BeforeEach`에서 생성자로 조립한다. 하위 implement는 실제 객체로 조립하고, 가장 바깥 의존(Repository, 외부 Client)만 mock으로 둔다.
- 스터빙과 검증은 BDDMockito로 한다: `given(..).willReturn(..)`, `then(..).should()`. `when(..)`은 쓰지 않는다.
- 단언은 AssertJ만 쓴다: `assertThat`, `assertThatThrownBy`.

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
