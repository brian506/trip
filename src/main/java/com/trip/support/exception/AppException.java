package com.trip.support.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final ErrorType errorType;
    private final Object data;

    public AppException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = null;
    }

    public AppException(ErrorType errorType, Object data) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
    }

    public AppException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
        this.data = null;
    }

    public AppException(ErrorType errorType, Object data, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
        this.data = data;
    }
}
