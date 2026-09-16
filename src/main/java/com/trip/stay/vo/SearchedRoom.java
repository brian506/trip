package com.trip.stay.vo;

import com.trip.supplier.Supplier;
import com.trip.supplier.vo.SupplierRoom;
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

    // 숙소·객실 정보는 DB 스냅샷(후보), 요금·재고는 공급사 응답에서 온다.
    public static SearchedRoom of(RoomOption option, SupplierRoom room) {
        return new SearchedRoom(
                option.stayId(),
                option.stayName(),
                option.roomTypeId(),
                option.roomTypeName(),
                option.maxOccupancy(),
                room.availableRooms(),
                room.supplier(),
                room.breakfastIncluded(),
                room.currency(),
                room.totalPrice()
        );
    }
}
