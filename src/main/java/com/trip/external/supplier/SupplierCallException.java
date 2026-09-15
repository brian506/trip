package com.trip.external.supplier;

import lombok.Getter;

// 공급사 호출 실패. 어댑터가 HTTP 상태·본문 코드를 내부 분류로 바꿔 던진다. 원본 코드는 code에 남긴다.
@Getter
public class SupplierCallException extends RuntimeException {

    private final Supplier supplier;
    private final SupplierFailureType type;
    private final String code;

    public SupplierCallException(Supplier supplier, SupplierFailureType type, String code) {
        super(supplier + " " + type + " " + code);
        this.supplier = supplier;
        this.type = type;
        this.code = code;
    }

    public SupplierCallException(Supplier supplier, SupplierFailureType type, String code, Throwable cause) {
        super(supplier + " " + type + " " + code, cause);
        this.supplier = supplier;
        this.type = type;
        this.code = code;
    }
}
