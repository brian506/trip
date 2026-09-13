package com.trip.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 통합 테스트는 ./gradlew integrationTest로만 실행된다. ./gradlew test는 이 태그를 제외한다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@SpringBootTest
public @interface IntegrationTest {
}
