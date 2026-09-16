package com.trip.stay.controller.response;

import com.trip.stay.vo.SearchedRoom;
import com.trip.supplier.vo.SupplierFailure;
import java.util.List;

public record StaySearchResponse(List<SearchedRoom> rooms, List<SupplierFailure> failures) {

    public StaySearchResponse {
        rooms = List.copyOf(rooms);
        failures = List.copyOf(failures);
    }
}
