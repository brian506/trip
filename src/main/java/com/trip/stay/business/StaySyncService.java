package com.trip.stay.business;

import com.trip.stay.implement.StayManager;
import com.trip.supplier.SupplierClient;
import com.trip.supplier.exception.SupplierCallException;
import com.trip.supplier.vo.SupplierStay;
import com.trip.support.exception.AppException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class StaySyncService {

    private final List<SupplierClient> clients;
    private final StayManager stayManager;

    public void syncAll() {
        for (SupplierClient client : clients) {
            sync(client);
        }
    }

    private void sync(SupplierClient client) {
        List<SupplierStay> supplierStays;
        try {
            supplierStays = client.fetchStays();
        } catch (SupplierCallException e) {
            log.warn("[숙소 동기화 : 목록 조회 실패]: supplier={} | type={} | code={}",
                    e.getSupplier(), e.getType(), e.getCode());
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
