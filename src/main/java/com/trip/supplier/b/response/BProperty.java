package com.trip.supplier.b.response;

import com.trip.supplier.vo.SupplierRoomType;
import com.trip.supplier.vo.SupplierStay;
import java.util.List;

public record BProperty(String propertyId, String propertyName, List<BRoom> rooms) {

    public SupplierStay toSupplierStay() {
        List<SupplierRoomType> supplierRoomTypes = rooms == null ? List.of() : rooms.stream()
                .map(BRoom::toSupplierRoomType)
                .toList();
        return new SupplierStay(propertyId, propertyName, supplierRoomTypes);
    }
}
