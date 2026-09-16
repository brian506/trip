package com.trip.supplier.vo;

import com.trip.supplier.Supplier;
import com.trip.supplier.exception.SupplierFailureType;

// 검색 중 실패한 공급사. 공급사 단위로 한 건만 남긴다. SupplierCallException.toFailure()가 만든다.
public record SupplierFailure(Supplier supplier, SupplierFailureType type, String code) {
}
