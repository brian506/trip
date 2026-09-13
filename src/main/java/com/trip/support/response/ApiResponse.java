package com.trip.support.response;

import com.trip.support.exception.ErrorMessage;
import com.trip.support.exception.ErrorType;

public record ApiResponse<T>(
        ResultType resultType,
        T data,
        ErrorMessage error
) {
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static <S> ApiResponse<S> error(ErrorType error) {
        return new ApiResponse<>(ResultType.ERROR, null, ErrorMessage.of(error, null));
    }

    public static <S> ApiResponse<S> error(ErrorType error, Object errorData) {
        return new ApiResponse<>(ResultType.ERROR, null, ErrorMessage.of(error, errorData));
    }
}
