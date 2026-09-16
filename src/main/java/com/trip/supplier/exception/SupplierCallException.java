package com.trip.supplier.exception;

import com.trip.supplier.Supplier;
import com.trip.supplier.vo.SupplierFailure;
import com.trip.support.exception.AppException;
import lombok.Getter;


@Getter
public class SupplierCallException extends AppException {

    private final Supplier supplier;
    private final SupplierFailureType type;
    private final String code;

    public SupplierCallException(Supplier supplier, SupplierFailureType type, String code) {
        this(supplier, type, code, null);
    }

    public SupplierCallException(Supplier supplier, SupplierFailureType type, String code, Throwable cause) {
        super(type.getErrorType(), supplier + " " + type + " " + code, cause);
        this.supplier = supplier;
        this.type = type;
        this.code = code;
    }

    // 검색은 공급사 하나가 실패해도 나머지 결과를 응답하므로, 예외를 값으로 바꿈
    public SupplierFailure toFailure() {
        return new SupplierFailure(supplier, type, code);
    }
}
