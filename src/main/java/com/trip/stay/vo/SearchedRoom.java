package com.trip.stay.vo;

import com.trip.supplier.Supplier;
import java.util.UUID;

public record SearchedRoom(
        UUID stayId,
        String stayName,
        UUID roomTypeId,
        String roomTypeName,
        int maxOccupancy,
        int availableRooms,
        Supplier supplier,
        boolean breakfastIncluded,
        String currency,
        long totalPrice
) {
}
