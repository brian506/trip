package com.trip.external.supplier;

import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import java.util.List;

// 공급사 숙소 목록 API가 준 숙소 한 건과 그 객실 타입들.
// 검증 실패 시 data에 숙소 코드와 사유를 담아 동기화 건너뜀 로그에 남긴다.
public record SupplierStay(String stayCode, String name, List<SupplierRoomType> roomTypes) {

    public SupplierStay {
        if (stayCode == null || stayCode.isBlank()) {
            throw invalid(stayCode, "숙소 코드 없음");
        }
        if (name == null || name.isBlank()) {
            throw invalid(stayCode, "숙소 이름 없음");
        }
        if (roomTypes == null) {
            throw invalid(stayCode, "객실 목록 없음");
        }
        roomTypes = List.copyOf(roomTypes);
    }

    private static AppException invalid(String stayCode, String reason) {
        return new AppException(ErrorType.INVALID_SUPPLIER_STAY, "stayCode=" + stayCode + ", " + reason);
    }
}
