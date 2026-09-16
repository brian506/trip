package com.trip.supplier;

import com.trip.supplier.vo.SupplierRoom;
import com.trip.supplier.vo.SupplierStay;
import com.trip.supplier.vo.SupplierStayCodes;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import java.util.List;


public interface SupplierClient {

    Supplier supplier();

    // 숙소 목록 API
    List<SupplierStay> fetchStays();

    // 재고/요금 API.
    List<SupplierRoom> fetchRooms(StayPeriod period, Guests guests, SupplierStayCodes stayCodes);
}
