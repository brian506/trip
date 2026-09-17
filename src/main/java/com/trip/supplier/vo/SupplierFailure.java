package com.trip.supplier.vo;

import com.trip.supplier.Supplier;
import com.trip.support.exception.supplier.SupplierFailureType;

public record SupplierFailure(Supplier supplier, SupplierFailureType type, String code) {
}
