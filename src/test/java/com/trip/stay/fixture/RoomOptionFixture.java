package com.trip.stay.fixture;

import com.trip.stay.vo.RoomOption;
import com.trip.supplier.Supplier;

import java.util.UUID;

public final class RoomOptionFixture {

    private RoomOptionFixture() {
    }

    public static RoomOption optionA() {
        return option(Supplier.A, "A-1001", "A-R01");
    }

    public static RoomOption optionB() {
        return option(Supplier.B, "B-2001", "B-R01");
    }

    public static RoomOption option(Supplier supplier, String stayCode, String roomTypeCode) {
        return new RoomOption(
                UUID.randomUUID(),
                stayCode,
                stayCode + " 숙소",
                supplier,
                UUID.randomUUID(),
                roomTypeCode,
                roomTypeCode + " 객실",
                supplier == Supplier.A ? 3 : 2
        );
    }

    public static RoomOption anotherRoomOf(RoomOption option, String roomTypeCode) {
        return new RoomOption(
                option.stayId(),
                option.stayCode(),
                option.stayName(),
                option.supplier(),
                UUID.randomUUID(),
                roomTypeCode,
                roomTypeCode + " 객실",
                option.maxOccupancy()
        );
    }
}
