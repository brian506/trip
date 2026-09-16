package com.trip.supplier.vo;

import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;


public record SupplierRoomType(String roomTypeCode, String name, int maxOccupancy) {

    public SupplierRoomType {
        if (roomTypeCode == null || roomTypeCode.isBlank()) {
            throw new AppException(ErrorType.INVALID_SUPPLIER_STAY, "roomTypeCode=" + roomTypeCode + ", 객실 코드 없음");
        }
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorType.INVALID_SUPPLIER_STAY, "roomTypeCode=" + roomTypeCode + ", 객실 이름 없음");
        }
        if (maxOccupancy < 1) {
            throw new AppException(ErrorType.INVALID_SUPPLIER_STAY, "roomTypeCode=" + roomTypeCode + ", 최대 인원 1 미만 (" + maxOccupancy + ")");
        }
    }
}
