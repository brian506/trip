package com.trip.supplier.a.response;

import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.vo.StayPeriod;

import java.util.ArrayList;
import java.util.List;

public record AAvailabilityResponse(List<AAvailabilityItem> items) {

    public List<SupplierRoom> toSupplierRooms(StayPeriod period) {
        List<SupplierRoom> rooms = new ArrayList<>();
        for (AAvailabilityItem item : items) {
            SupplierRoom room = item.toSupplierRoom(period);
            if (room == null) {
                continue;
            }
            rooms.add(room);
        }
        return List.copyOf(rooms);
    }
}
