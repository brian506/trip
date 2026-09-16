package com.trip.supplier.b.response;

// B는 실패해도 HTTP 200, resultCode가 0000이 아니면 실패
public record BResponse<T>(String resultCode, String resultMessage, T data) {

    public static final String SUCCESS_CODE = "0000";

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(resultCode);
    }
}
