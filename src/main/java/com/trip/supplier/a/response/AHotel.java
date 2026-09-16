package com.trip.supplier.a.response;

import com.trip.supplier.vo.SupplierRoomType;
import com.trip.supplier.vo.SupplierStay;
import java.util.List;

public record AHotel(String hotelCode, String hotelName, List<ARoomType> roomTypes) {

    public SupplierStay toSupplierStay() {
        List<SupplierRoomType> supplierRoomTypes = roomTypes == null ? List.of() : roomTypes.stream()
                .map(ARoomType::toSupplierRoomType)
                .toList();
        return new SupplierStay(hotelCode, hotelName, supplierRoomTypes);
    }
}
