package com.trip.supplier;

import com.trip.supplier.vo.SupplierStay;
import java.util.List;


public interface SupplierClient {

    Supplier supplier();

    // 숙소 목록 API
    List<SupplierStay> fetchStays();
}
