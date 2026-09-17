package com.trip.supplier.global;

import java.time.Duration;

// 숙소 목록 동기화에만 건다. 사용자가 기다리는 호출이 아니라 대기 시간을 넉넉히 줄 수 있다.
public record SupplierRetrySettings(
        int maxAttempts,
        Duration waitDuration,
        double backoffMultiplier
) {
}
