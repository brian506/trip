package com.trip.supplier.b.response;

import com.trip.supplier.Supplier;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.vo.StayPeriod;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record BSearchItem(
        String propertyId,
        String propertyName,
        String roomId,
        String roomName,
        Integer maxOccupancy,
        Boolean breakfastIncluded,
        String currency,
        Long totalPrice,
        Boolean taxIncluded,
        List<BInventory> inventory
) {

    public SupplierRoom toSupplierRoom(StayPeriod period) {
        if (totalPrice == null || inventory == null) {
            return null;
        }
        Map<LocalDate, BInventory> inventoryByDate = new HashMap<>();
        for (BInventory daily : inventory) {
            if (daily != null && daily.date() != null) {
                inventoryByDate.putIfAbsent(daily.date(), daily);
            }
        }
        int availableRooms = Integer.MAX_VALUE;
        for (LocalDate date : period.dates()) {
            BInventory daily = inventoryByDate.get(date);
            if (daily == null || daily.remainingRooms() == null) {
                return null;
            }
            availableRooms = Math.min(availableRooms, daily.remainingRooms());
        }
        return new SupplierRoom(Supplier.B, propertyId, roomId, availableRooms,
                Boolean.TRUE.equals(breakfastIncluded), currency, totalPrice);
    }
}
