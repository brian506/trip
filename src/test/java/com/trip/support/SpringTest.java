package com.trip.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// 서비스와 H2를 엮는 통합 테스트의 베이스. 테스트마다 트랜잭션을 롤백한다.
// 테스트 스레드 밖(별도 스레드, Reactor)에서 커밋된 데이터는 롤백되지 않는다.
@IntegrationTest
@ActiveProfiles("test")
@Transactional
public abstract class SpringTest {
}
