package com.trip.supplier.fixture;

import com.trip.stay.vo.RoomOption;
import com.trip.supplier.Supplier;
import com.trip.supplier.vo.SupplierRoom;

public final class SupplierRoomFixture {

    public static final int AVAILABLE_ROOMS = 4;
    public static final boolean BREAKFAST_INCLUDED = true;
    public static final String CURRENCY = "KRW";
    public static final long TOTAL_PRICE = 275_000L;

    private SupplierRoomFixture() {
    }

    public static SupplierRoom roomFor(RoomOption option) {
        return room(option.supplier(), option.stayCode(), option.roomTypeCode());
    }

    public static SupplierRoom roomFor(RoomOption option, int availableRooms, long totalPrice) {
        return new SupplierRoom(
                option.supplier(),
                option.stayCode(),
                option.roomTypeCode(),
                availableRooms,
                BREAKFAST_INCLUDED,
                CURRENCY,
                totalPrice
        );
    }

    public static SupplierRoom room(Supplier supplier, String stayCode, String roomTypeCode) {
        return new SupplierRoom(
                supplier,
                stayCode,
                roomTypeCode,
                AVAILABLE_ROOMS,
                BREAKFAST_INCLUDED,
                CURRENCY,
                TOTAL_PRICE
        );
    }
}
