package com.trip.common.controller;

import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorCode;
import com.trip.support.exception.ErrorType;
import com.trip.support.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;

@Slf4j
@RestControllerAdvice
public class ApiControllerAdvice {

    @NullMarked
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleAppException(AppException e) {
        printAppExceptionLog(e);
        return new ResponseEntity<>(ApiResponse.error(e.getErrorType(), e.getData()), e.getErrorType().getStatus());
    }

    @NullMarked
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = fieldErrors(e);
        log.warn("[요청 검증 : 실패]: {}", format(fieldErrors));
        return new ResponseEntity<>(ApiResponse.error(ErrorType.INVALID_ACCESS_PATH, fieldErrors), HttpStatus.BAD_REQUEST);
    }

    @NullMarked
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<@Nullable Object>> handleRequestParamException(Exception e) {
        log.warn("[요청 파라미터 : 바인딩 실패]: message={}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ErrorType.INVALID_ACCESS_PATH, null), HttpStatus.BAD_REQUEST);
    }

    @NullMarked
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("[요청 파라미터 : 제약 위반]: message={}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ErrorType.INVALID_ACCESS_PATH, null), HttpStatus.BAD_REQUEST);
    }

    @NullMarked
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleException(Exception e) {
        log.error("[미처리 예외 : 시스템 오류]: type={} | message={}", e.getClass().getName(), e.getMessage(), e);
        return new ResponseEntity<>(ApiResponse.error(ErrorType.DEFAULT_ERROR, null), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 같은 필드에 제약이 여러 개 걸리면 첫 사유만 남긴다.
    private Map<String, String> fieldErrors(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            String message = error.getDefaultMessage();
            if (error.isBindingFailure() || message == null) {
                message = "형식이 올바르지 않습니다";
            }
            fieldErrors.putIfAbsent(error.getField(), message);
        }
        return fieldErrors;
    }

    private String format(Map<String, String> fieldErrors) {
        return fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" | "));
    }

    private void printAppExceptionLog(AppException e) {
        StackTraceElement origin = e.getStackTrace()[0];
        int status = e.getErrorType().getStatus().value();
        ErrorCode errorCode = e.getErrorType().getErrorCode();

        switch (e.getErrorType().getLogLevel()) {
            case ERROR -> log.error("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                    origin.getClassName(), origin.getMethodName(), origin.getLineNumber(), status, errorCode, e.getMessage(), e.getData());
            case WARN -> log.warn("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                    origin.getClassName(), origin.getMethodName(), origin.getLineNumber(), status, errorCode, e.getMessage(), e.getData());
            default -> log.info("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                    origin.getClassName(), origin.getMethodName(), origin.getLineNumber(), status, errorCode, e.getMessage(), e.getData());
        }
    }
}
