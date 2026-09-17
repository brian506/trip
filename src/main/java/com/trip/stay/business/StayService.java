package com.trip.stay.business;

import com.trip.stay.controller.response.StaySearchResponse;
import com.trip.stay.implement.StayManager;
import com.trip.stay.vo.RoomOption;
import com.trip.supplier.SupplierClient;
import com.trip.support.exception.supplier.SupplierCallException;
import com.trip.supplier.global.SupplierDispatcher;
import com.trip.supplier.vo.SupplierDispatchResult;
import com.trip.supplier.vo.SupplierStay;
import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StayService {

    private final List<SupplierClient> clients;
    private final StayManager stayManager;
    private final SupplierDispatcher supplierDispatcher;

    public StaySearchResponse search(StayPeriod period, Guests guests) {
        List<RoomOption> options = stayManager.findActiveRooms(guests);
        if (options.isEmpty()) {
            return new StaySearchResponse(List.of(), List.of());
        }

        SupplierDispatchResult result = supplierDispatcher.dispatch(period, guests, stayManager.toStayCodes(options));
        if (result.allFailed()) {
            throw new AppException(ErrorType.SUPPLIER_ALL_FAILED, result.failures());
        }
        return new StaySearchResponse(stayManager.toSearchedRooms(options, result.rooms()), result.failures());
    }

    public void syncAll() {
        clients.forEach(this::sync);
    }

    private void sync(SupplierClient client) {
        List<SupplierStay> supplierStays;
        try {
            supplierStays = client.fetchStays();
        } catch (SupplierCallException e) {
            log.warn("[숙소 동기화 : 목록 조회 실패]: supplier={} | type={} | code={}",
                    e.getSupplier(), e.getType(), e.getCode(), e);
            return;
        } catch (AppException e) {
            log.warn("[숙소 동기화 : 항목 검증 실패]: supplier={} | detail={}", client.supplier(), e.getData());
            return;
        }
        if (supplierStays.isEmpty()) {
            log.warn("[숙소 동기화 : 빈 목록]: supplier={}", client.supplier());
            return;
        }
        try {
            stayManager.sync(client.supplier(), supplierStays);
        } catch (RuntimeException e) {
            log.error("[숙소 동기화 : 저장 실패]: supplier={}", client.supplier(), e);
        }
    }
}
