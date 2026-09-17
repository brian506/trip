package com.trip.support.exception.supplier;

import com.trip.supplier.Supplier;
import io.netty.channel.ConnectTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.Exceptions;

// 공급사 호출 실패 분류 규칙.
public final class SupplierErrors {

    private SupplierErrors() {
    }

    public static SupplierFailureType classify(HttpStatusCode status) {
        if (status.value() == 401) {
            return SupplierFailureType.AUTH;
        }
        if (status.value() == 429) {
            return SupplierFailureType.RATE_LIMITED;
        }
        if (status.value() == 503) {
            return SupplierFailureType.UNAVAILABLE;
        }
        if (status.is4xxClientError()) {
            return SupplierFailureType.BAD_REQUEST;
        }
        return SupplierFailureType.INTERNAL;
    }

    public static SupplierCallException translate(Supplier supplier, RuntimeException e) {
        Throwable cause = Exceptions.unwrap(e);
        if (isPoolAcquireFailure(cause)) {
            return new SupplierCallException(supplier, SupplierFailureType.UNAVAILABLE, "POOL_EXHAUSTED", cause);
        }
        if (hasCause(cause, TimeoutException.class, io.netty.handler.timeout.TimeoutException.class)) {
            return new SupplierCallException(supplier, SupplierFailureType.UNAVAILABLE, "TIMEOUT", cause);
        }
        if (hasCause(cause, DataBufferLimitException.class)) {
            return new SupplierCallException(supplier, SupplierFailureType.MALFORMED, "BODY_TOO_LARGE", cause);
        }
        if (cause instanceof WebClientRequestException) {
            return new SupplierCallException(supplier, SupplierFailureType.UNAVAILABLE, connectCode(cause), cause);
        }
        return new SupplierCallException(supplier, SupplierFailureType.MALFORMED, "UNEXPECTED", cause);
    }

    // 연결 실패는 원인마다 대응이 달라서 코드를 나눈다. 느린 것(재시도 여지)과 죽은 것(설정·장애)을 로그 한 줄로 가른다.
    private static String connectCode(Throwable cause) {
        // ConnectTimeoutException은 ConnectException의 하위 타입이라 먼저 본다.
        if (hasCause(cause, ConnectTimeoutException.class)) {
            return "CONNECT_TIMEOUT";
        }
        if (hasCause(cause, UnknownHostException.class)) {
            return "DNS";
        }
        if (hasCause(cause, ConnectException.class)) {
            return "CONNECT_REFUSED";
        }
        return "CONNECTION";
    }

    private static boolean isPoolAcquireFailure(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c.getClass().getSimpleName().startsWith("PoolAcquire")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCause(Throwable t, Class<?>... types) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            for (Class<?> type : types) {
                if (type.isInstance(c)) {
                    return true;
                }
            }
        }
        return false;
    }
}
