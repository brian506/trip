package com.trip.external.supplier;

import java.util.concurrent.TimeoutException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.Exceptions;

// 공급사 호출 실패 분류 규칙. HTTP 상태 분류는 Client가, 전송 오류 변환은 SupplierHttpCaller가 쓴다.
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

    // 응답을 끝까지 받지 못해 block()이 던진 예외를 내부 분류로 바꾼다.
    // 타임아웃은 WebClientRequestException 안에 감싸여 올 수 있어 연결 오류보다 먼저 본다.
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
