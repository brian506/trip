package com.trip.supplier.vo;

import com.trip.supplier.Supplier;
import com.trip.supplier.exception.SupplierFailureType;

public record SupplierFailure(Supplier supplier, SupplierFailureType type, String code) {
}
