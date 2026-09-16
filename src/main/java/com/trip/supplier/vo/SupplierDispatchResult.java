package com.trip.supplier.vo;

import java.util.List;

public record SupplierDispatchResult(List<SupplierRoom> rooms, List<SupplierFailure> failures, int dispatchedSuppliers) {

    public SupplierDispatchResult {
        rooms = List.copyOf(rooms);
        failures = List.copyOf(failures);
    }

    public static SupplierDispatchResult empty() {
        return new SupplierDispatchResult(List.of(), List.of(), 0);
    }

    public boolean allFailed() {
        return dispatchedSuppliers > 0 && failures.size() == dispatchedSuppliers;
    }
}
