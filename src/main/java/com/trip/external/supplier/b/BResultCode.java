package com.trip.external.supplier.b;

import com.trip.external.supplier.SupplierFailureType;
import java.util.Arrays;

// B 본문 resultCode 중 실패 코드와 내부 실패 분류의 대응. 성공 코드는 BResponse가 판단한다.
public enum BResultCode {

    BAD_REQUEST("E400", SupplierFailureType.BAD_REQUEST),
    AUTH("E401", SupplierFailureType.AUTH),
    RATE_LIMITED("E429", SupplierFailureType.RATE_LIMITED),
    UNAVAILABLE("E503", SupplierFailureType.UNAVAILABLE);

    private final String code;
    private final SupplierFailureType failureType;

    BResultCode(String code, SupplierFailureType failureType) {
        this.code = code;
        this.failureType = failureType;
    }

    // 코드가 없으면 응답 이상(MALFORMED), 목록에 없는 코드는 공급사 내부 오류(INTERNAL)로 본다.
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
