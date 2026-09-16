package com.trip.support.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {

    // Global
    INVALID_ACCESS_PATH(HttpStatus.BAD_REQUEST, ErrorCode.E400, "요청 값이 올바르지 않습니다.", LogLevel.WARN),
    NOT_FOUND_DATA(HttpStatus.BAD_REQUEST, ErrorCode.E400, "해당 데이터를 찾을 수 없습니다.", LogLevel.WARN),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, ErrorCode.E409, "이미 등록된 정보입니다.", LogLevel.WARN),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.E429, "너무 많은 요청을 보냈습니다.", LogLevel.WARN),
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),

    // Stay
    INVALID_SUPPLIER_STAY(HttpStatus.BAD_GATEWAY, ErrorCode.E1000, "공급사 숙소 정보가 올바르지 않습니다.", LogLevel.WARN),

    // Supplier
    SUPPLIER_BAD_REQUEST(HttpStatus.BAD_GATEWAY, ErrorCode.E2000, "공급사 요청이 올바르지 않습니다.", LogLevel.WARN),
    SUPPLIER_AUTH(HttpStatus.BAD_GATEWAY, ErrorCode.E2001, "공급사 인증에 실패했습니다.", LogLevel.WARN),
    SUPPLIER_RATE_LIMITED(HttpStatus.BAD_GATEWAY, ErrorCode.E2002, "공급사 호출 한도를 넘었습니다.", LogLevel.WARN),
    SUPPLIER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, ErrorCode.E2003, "공급사에 연결할 수 없습니다.", LogLevel.WARN),
    SUPPLIER_INTERNAL(HttpStatus.BAD_GATEWAY, ErrorCode.E2004, "공급사에서 오류가 발생했습니다.", LogLevel.WARN),
    SUPPLIER_MALFORMED(HttpStatus.BAD_GATEWAY, ErrorCode.E2005, "공급사 응답을 해석할 수 없습니다.", LogLevel.WARN);

    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final String message;
    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode errorCode, String message, LogLevel logLevel) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.logLevel = logLevel;
    }
}
