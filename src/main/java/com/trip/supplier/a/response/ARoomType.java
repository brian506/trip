package com.trip.supplier.a.response;

import com.trip.supplier.vo.SupplierRoomType;

public record ARoomType(String roomTypeCode, String roomTypeName, Integer maxOccupancy) {

    public SupplierRoomType toSupplierRoomType() {
        return new SupplierRoomType(roomTypeCode, roomTypeName, maxOccupancy == null ? 0 : maxOccupancy);
    }
}
