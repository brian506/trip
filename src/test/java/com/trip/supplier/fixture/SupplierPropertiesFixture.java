package com.trip.supplier.fixture;

import com.trip.supplier.Supplier;
import com.trip.supplier.global.SupplierCircuitBreakerSettings;
import com.trip.supplier.global.SupplierEndpoint;
import com.trip.supplier.global.SupplierProperties;
import com.trip.supplier.global.SupplierRetrySettings;

import java.time.Duration;
import java.util.Map;

public final class SupplierPropertiesFixture {

    private SupplierPropertiesFixture() {
    }

    // 브레이커는 열리지 않고 재시도는 10ms만 기다린다. 재시도·차단 자체는 테스트 대상이 아니다.
    public static SupplierProperties properties(Duration totalTimeout) {
        return new SupplierProperties(
                Map.of(
                        Supplier.A, new SupplierEndpoint("http://localhost:1", "key-a"),
                        Supplier.B, new SupplierEndpoint("http://localhost:2", "key-b")
                ),
                Duration.ofMillis(200),
                Duration.ofMillis(500),
                50,
                Duration.ofMillis(200),
                Duration.ofSeconds(30),
                totalTimeout,
                new SupplierCircuitBreakerSettings(20, 1_000, 50.0f, Duration.ofSeconds(10), 3),
                new SupplierRetrySettings(3, Duration.ofMillis(10), 2.0)
        );
    }
}
