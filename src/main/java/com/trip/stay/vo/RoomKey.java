package com.trip.stay.vo;

import com.trip.supplier.Supplier;
import com.trip.supplier.vo.SupplierRoom;

public record RoomKey(Supplier supplier, String stayCode, String roomTypeCode) {

    public static RoomKey from(RoomOption option) {
        return new RoomKey(option.supplier(), option.stayCode(), option.roomTypeCode());
    }

    public static RoomKey from(SupplierRoom room) {
        return new RoomKey(room.supplier(), room.stayCode(), room.roomTypeCode());
    }
}
