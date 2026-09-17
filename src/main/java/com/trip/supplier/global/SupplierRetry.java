package com.trip.supplier.global;

import com.trip.supplier.Supplier;
import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import com.trip.support.exception.supplier.SupplierCallException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SupplierRetry {

    private final Map<Supplier, Retry> retries;

    public SupplierRetry(SupplierProperties properties) {
        SupplierRetrySettings settings = properties.retry();
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(settings.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        settings.waitDuration(), settings.backoffMultiplier()))
                .retryOnException(SupplierRetry::indicatesSupplierDown)
                .build();

        this.retries = Arrays.stream(Supplier.values())
                .collect(Collectors.toUnmodifiableMap(Function.identity(), supplier -> create(supplier, config)));
    }

    public <T> T call(Supplier supplier, Callable<T> call) {
        try {
            return retries.get(supplier).executeCallable(call);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorType.DEFAULT_ERROR, e);
        }
    }

    private static Retry create(Supplier supplier, RetryConfig config) {
        Retry retry = Retry.of(supplier.name(), config);
        retry.getEventPublisher().onRetry(event -> log.warn(
                "[공급사 호출 재시도 : 대기 후 재시도]: supplier={} | attempt={} | wait={} | cause={}",
                supplier, event.getNumberOfRetryAttempts(), event.getWaitInterval(),
                event.getLastThrowable() == null ? null : event.getLastThrowable().getMessage()));
        return retry;
    }

    private static boolean indicatesSupplierDown(Throwable e) {
        return e instanceof SupplierCallException failure && failure.getType().indicatesSupplierDown();
    }
}
