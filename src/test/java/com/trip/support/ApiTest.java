package com.trip.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// API를 호출하고 돌아온 값만 본다. DB는 들여다보지 않고, 동기화의 결과는 이어지는 검색 응답으로 확인한다.
@AutoConfigureMockMvc
public abstract class ApiTest extends SpringTest {

    protected static final SupplierMockServer supplierServer = new SupplierMockServer();

    @Autowired
    protected MockMvc mockMvc;

    // API 키도 테스트 전용 값으로 덮는다. 운영 설정값을 그대로 쓰면 설정을 안 읽고 문자열을 박아 넣어도 통과한다.
    @DynamicPropertySource
    static void supplierEndpoints(DynamicPropertyRegistry registry) {
        registry.add("supplier.endpoints.A.base-url", supplierServer::baseUrl);
        registry.add("supplier.endpoints.B.base-url", supplierServer::baseUrl);
        registry.add("supplier.endpoints.A.api-key", () -> "test-key-a");
        registry.add("supplier.endpoints.B.api-key", () -> "test-key-b");
    }

    @AfterEach
    void resetSupplierServer() {
        supplierServer.reset();
    }
}
