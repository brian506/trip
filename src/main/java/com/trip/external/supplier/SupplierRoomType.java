package com.trip.external.supplier;

import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;

// 공급사 숙소 목록 API가 준 객실 타입 한 건. Client가 공급사 응답을 이 형태로 바꿔 넘긴다.
// 검증 실패 시 data에 객실 코드와 사유를 담는다. 어느 숙소의 객실인지는 이 VO가 알지 못한다.
public record SupplierRoomType(String roomTypeCode, String name, int maxOccupancy) {

    public SupplierRoomType {
        if (roomTypeCode == null || roomTypeCode.isBlank()) {
            throw invalid(roomTypeCode, "객실 코드 없음");
        }
        if (name == null || name.isBlank()) {
            throw invalid(roomTypeCode, "객실 이름 없음");
        }
        if (maxOccupancy < 1) {
            throw invalid(roomTypeCode, "최대 인원 1 미만 (" + maxOccupancy + ")");
        }
    }

    private static AppException invalid(String roomTypeCode, String reason) {
        return new AppException(ErrorType.INVALID_SUPPLIER_STAY, "roomTypeCode=" + roomTypeCode + ", " + reason);
    }
}
