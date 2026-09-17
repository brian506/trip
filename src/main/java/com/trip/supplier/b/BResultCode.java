package com.trip.supplier.b;

import com.trip.support.exception.supplier.SupplierFailureType;
import java.util.Arrays;

public enum BResultCode {

    BAD_REQUEST("E400", SupplierFailureType.BAD_REQUEST),
    AUTH("E401", SupplierFailureType.AUTH),
    RATE_LIMITED("E429", SupplierFailureType.RATE_LIMITED),
    INTERNAL("E500", SupplierFailureType.INTERNAL),
    UNAVAILABLE("E503", SupplierFailureType.UNAVAILABLE);

    private final String code;
    private final SupplierFailureType failureType;

    BResultCode(String code, SupplierFailureType failureType) {
        this.code = code;
        this.failureType = failureType;
    }

    public static SupplierFailureType classify(String resultCode) {
        if (resultCode == null) {
            return SupplierFailureType.MALFORMED;
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equals(resultCode))
                .findFirst()
                .map(value -> value.failureType)
                .orElse(SupplierFailureType.INTERNAL);
    }
}
