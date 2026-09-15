package com.trip.external.supplier;

import java.util.List;


public interface SupplierClient {

    Supplier supplier();

    // 숙소 목록 API
    List<SupplierStay> fetchStays();
}
