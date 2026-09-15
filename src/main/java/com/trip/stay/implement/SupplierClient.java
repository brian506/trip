package com.trip.stay.implement;

import com.trip.stay.vo.SupplierStay;
import com.trip.stay.vo.Supplier;
import java.util.List;


public interface SupplierClient {

    Supplier supplier();

    // 숙소 목록 API
    List<SupplierStay> fetchStays();
}
