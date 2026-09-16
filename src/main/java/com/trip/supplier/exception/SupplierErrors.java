package com.trip.supplier.exception;

import com.trip.supplier.Supplier;
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
        if (hasCause(cause, TimeoutException.class, io.netty.handler.timeout.TimeoutException.class)) {
            return new SupplierCallException(supplier, SupplierFailureType.UNAVAILABLE, "TIMEOUT", cause);
        }
        if (hasCause(cause, DataBufferLimitException.class)) {
            return new SupplierCallException(supplier, SupplierFailureType.MALFORMED, "BODY_TOO_LARGE", cause);
        }
        if (cause instanceof WebClientRequestException) {
            return new SupplierCallException(supplier, SupplierFailureType.UNAVAILABLE, "CONNECTION", cause);
        }
        return new SupplierCallException(supplier, SupplierFailureType.MALFORMED, "UNEXPECTED", cause);
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
