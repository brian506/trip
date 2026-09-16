package com.trip.supplier.vo;

import com.trip.supplier.Supplier;

public record SupplierRoom(
        Supplier supplier,
        String stayCode,
        String roomTypeCode,
        int availableRooms,
        boolean breakfastIncluded,
        String currency,
        long totalPrice
) {
}
