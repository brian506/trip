package com.trip.supplier.b.response;

import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.vo.StayPeriod;

import java.util.ArrayList;
import java.util.List;

public record BSearchData(List<BSearchItem> items) {

    public List<SupplierRoom> toSupplierRooms(StayPeriod period) {
        List<SupplierRoom> rooms = new ArrayList<>();
        for (BSearchItem item : items) {
            SupplierRoom room = item.toSupplierRoom(period);
            if (room == null) {
                continue;
            }
            rooms.add(room);
        }
        return List.copyOf(rooms);
    }
}
