package com.trip.stay.vo;

import com.trip.supplier.vo.SupplierFailure;
import java.util.List;

public record StaySearchResult(List<SearchedRoom> rooms, List<SupplierFailure> failures) {

    public StaySearchResult {
        rooms = List.copyOf(rooms);
        failures = List.copyOf(failures);
    }
}
