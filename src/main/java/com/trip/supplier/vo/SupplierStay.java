package com.trip.supplier.vo;

import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import java.util.List;

public record SupplierStay(String stayCode, String name, List<SupplierRoomType> roomTypes) {

    public SupplierStay {
        if (stayCode == null || stayCode.isBlank()) {
            throw new AppException(ErrorType.INVALID_SUPPLIER_STAY, "stayCode=" + stayCode + ", 숙소 코드 없음");
        }
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorType.INVALID_SUPPLIER_STAY, "stayCode=" + stayCode + ", 숙소 이름 없음");
        }
        if (roomTypes == null) {
            throw new AppException(ErrorType.INVALID_SUPPLIER_STAY, "stayCode=" + stayCode + ", 객실 목록 없음");
        }
        roomTypes = List.copyOf(roomTypes);
    }
}
