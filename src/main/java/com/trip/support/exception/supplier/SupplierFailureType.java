package com.trip.support.exception.supplier;

import com.trip.support.exception.ErrorType;
import lombok.Getter;

@Getter
public enum SupplierFailureType {

    BAD_REQUEST(ErrorType.SUPPLIER_BAD_REQUEST),
    AUTH(ErrorType.SUPPLIER_AUTH),
    RATE_LIMITED(ErrorType.SUPPLIER_RATE_LIMITED),
    UNAVAILABLE(ErrorType.SUPPLIER_UNAVAILABLE),
    INTERNAL(ErrorType.SUPPLIER_INTERNAL),
    MALFORMED(ErrorType.SUPPLIER_MALFORMED);

    private final ErrorType errorType;

    SupplierFailureType(ErrorType errorType) {
        this.errorType = errorType;
    }

    // 다음 묶음을 불러도 같은 결과인 실패. 남은 묶음을 부르지 않고 그 공급사를 끝낸다.
    public boolean stopsRemainingBatches() {
        return this == AUTH || this == RATE_LIMITED || this == UNAVAILABLE;
    }
}
