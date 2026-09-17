package com.trip.supplier.global;

import java.time.Duration;

public record SupplierCircuitBreakerSettings(
        int slidingWindowSize,
        int minimumNumberOfCalls,
        float failureRateThreshold,
        Duration waitDurationInOpenState,
        int permittedNumberOfCallsInHalfOpenState
) {
}
