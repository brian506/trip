package com.trip.stay.vo;

import com.trip.stay.dataaccess.entity.RoomType;
import com.trip.stay.dataaccess.entity.Stay;
import com.trip.supplier.Supplier;
import java.util.UUID;

public record RoomOption(
        UUID stayId,
        String stayCode,
        String stayName,
        Supplier supplier,
        UUID roomTypeId,
        String roomTypeCode,
        String roomTypeName,
        int maxOccupancy
) {

    public static RoomOption from(Stay stay, RoomType roomType) {
        return new RoomOption(
                stay.getId(),
                stay.getStayCode(),
                stay.getName(),
                stay.getSupplier(),
                roomType.getId(),
                roomType.getRoomTypeCode(),
                roomType.getName(),
                roomType.getMaxOccupancy()
        );
    }
}
