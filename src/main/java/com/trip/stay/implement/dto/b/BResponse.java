package com.trip.stay.implement.dto.b;

// B는 실패해도 HTTP 200이다. resultCode가 "0000"이 아니면 실패이고 data는 null이다.
public record BResponse<T>(String resultCode, String resultMessage, T data) {

    public static final String SUCCESS_CODE = "0000";

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(resultCode);
    }
}
