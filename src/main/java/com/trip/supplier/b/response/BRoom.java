package com.trip.supplier.b.response;

import com.trip.supplier.vo.SupplierRoomType;

public record BRoom(String roomId, String roomName, Integer maxOccupancy) {

    public SupplierRoomType toSupplierRoomType() {
        return new SupplierRoomType(roomId, roomName, maxOccupancy == null ? 0 : maxOccupancy);
    }
}
