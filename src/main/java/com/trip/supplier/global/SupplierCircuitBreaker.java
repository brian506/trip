package com.trip.supplier.global;

import com.trip.supplier.Supplier;
import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import com.trip.support.exception.supplier.SupplierCallException;
import com.trip.support.exception.supplier.SupplierFailureType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SupplierCircuitBreaker {

    private static final String CIRCUIT_OPEN = "CIRCUIT_OPEN";

    private final Map<Supplier, CircuitBreaker> breakers;

    public SupplierCircuitBreaker(SupplierProperties properties) {
        SupplierCircuitBreakerSettings settings = properties.circuitBreaker();
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(settings.slidingWindowSize())
                .minimumNumberOfCalls(settings.minimumNumberOfCalls())
                .failureRateThreshold(settings.failureRateThreshold())
                .waitDurationInOpenState(settings.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(settings.permittedNumberOfCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(false)
                .ignoreException(SupplierCircuitBreaker::doesNotIndicateSupplierDown)
                .build();

        this.breakers = Arrays.stream(Supplier.values())
                .collect(Collectors.toUnmodifiableMap(Function.identity(), supplier -> create(supplier, config)));
    }

    public <T> T call(Supplier supplier, Callable<T> call) {
        CircuitBreaker breaker = breakers.get(supplier);
        if (!breaker.tryAcquirePermission()) {
            log.warn("[공급사 서킷 브레이커 : 호출 차단]: supplier={} | state={}", supplier, breaker.getState());
            throw new SupplierCallException(supplier, SupplierFailureType.UNAVAILABLE, CIRCUIT_OPEN);
        }
        long start = System.nanoTime();
        try {
            T result = call.call();
            breaker.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            return result;
        } catch (RuntimeException e) {
            breaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, e);
            throw e;
        } catch (Exception e) {
            breaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, e);
            throw new AppException(ErrorType.DEFAULT_ERROR, e);
        }
    }

    private static CircuitBreaker create(Supplier supplier, CircuitBreakerConfig config) {
        CircuitBreaker breaker = CircuitBreaker.of(supplier.name(), config);
        breaker.getEventPublisher().onStateTransition(event -> log.warn(
                "[공급사 서킷 브레이커 : 상태 전이]: supplier={} | from={} | to={} | failureRate={}",
                supplier,
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState(),
                breaker.getMetrics().getFailureRate()));
        return breaker;
    }

    private static boolean doesNotIndicateSupplierDown(Throwable e) {
        return !(e instanceof SupplierCallException failure) || !failure.getType().indicatesSupplierDown();
    }
}
