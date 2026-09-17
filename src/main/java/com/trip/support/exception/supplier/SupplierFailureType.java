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

    public boolean indicatesSupplierDown() {
        return this == UNAVAILABLE || this == INTERNAL;
    }

    public boolean stopsRemainingBatches() {
        return this == AUTH || this == RATE_LIMITED || this == UNAVAILABLE;
    }
}
