package com.trip.stay.vo;

// 공급사 호출 실패의 내부 분류. HTTP 상태와 본문 코드를 어댑터가 이 분류로 통일한다.
public enum SupplierFailureType {
    BAD_REQUEST,
    AUTH,
    RATE_LIMITED,
    UNAVAILABLE,
    INTERNAL,
    MALFORMED
}
