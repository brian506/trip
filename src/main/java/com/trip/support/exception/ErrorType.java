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
    INVALID_STAY_PERIOD(HttpStatus.BAD_REQUEST, ErrorCode.E1001, "체크인·체크아웃 날짜가 필요하며, 체크아웃은 체크인 이후여야 합니다.", LogLevel.WARN),
    PAST_CHECK_IN(HttpStatus.BAD_REQUEST, ErrorCode.E1002, "체크인은 오늘 이후여야 합니다.", LogLevel.WARN),
    STAY_PERIOD_TOO_LONG(HttpStatus.BAD_REQUEST, ErrorCode.E1003, "숙박은 최대 30박까지 검색할 수 있습니다.", LogLevel.WARN),
    INVALID_GUESTS(HttpStatus.BAD_REQUEST, ErrorCode.E1004, "성인은 1명 이상, 아동은 0명 이상이어야 합니다.", LogLevel.WARN),

    // Supplier
    SUPPLIER_BAD_REQUEST(HttpStatus.BAD_GATEWAY, ErrorCode.E2000, "공급사 요청이 올바르지 않습니다.", LogLevel.WARN),
    SUPPLIER_AUTH(HttpStatus.BAD_GATEWAY, ErrorCode.E2001, "공급사 인증에 실패했습니다.", LogLevel.WARN),
    SUPPLIER_RATE_LIMITED(HttpStatus.BAD_GATEWAY, ErrorCode.E2002, "공급사 호출 한도를 넘었습니다.", LogLevel.WARN),
    SUPPLIER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, ErrorCode.E2003, "공급사에 연결할 수 없습니다.", LogLevel.WARN),
    SUPPLIER_INTERNAL(HttpStatus.BAD_GATEWAY, ErrorCode.E2004, "공급사에서 오류가 발생했습니다.", LogLevel.WARN),
    SUPPLIER_MALFORMED(HttpStatus.BAD_GATEWAY, ErrorCode.E2005, "공급사 응답을 해석할 수 없습니다.", LogLevel.WARN),
    SUPPLIER_ALL_FAILED(HttpStatus.BAD_GATEWAY, ErrorCode.E2006, "모든 공급사 호출이 실패했습니다.", LogLevel.WARN);

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
