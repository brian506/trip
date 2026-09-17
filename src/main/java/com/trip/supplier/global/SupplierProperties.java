package com.trip.supplier.global;

import com.trip.supplier.Supplier;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supplier")
public record SupplierProperties(
        Map<Supplier, SupplierEndpoint> endpoints,
        Duration connectTimeout,
        Duration responseTimeout,
        int maxConnections,
        Duration pendingAcquireTimeout,
        Duration maxIdleTime,
        Duration totalTimeout,
        SupplierCircuitBreakerSettings circuitBreaker,
        SupplierRetrySettings retry
) {

    public SupplierEndpoint connectEndpoint(Supplier supplier) {
        SupplierEndpoint endpoint = endpoints == null ? null : endpoints.get(supplier);
        if (endpoint == null) {
            throw new IllegalStateException("supplier.endpoints." + supplier + " 설정이 없습니다.");
        }
        return endpoint;
    }
}
