package com.trip.stay.implement;

import com.trip.stay.dataaccess.entity.Stay;
import com.trip.stay.dataaccess.repository.StayRepository;
import com.trip.stay.vo.RoomOption;
import com.trip.support.vo.Guests;
import com.trip.supplier.Supplier;
import com.trip.supplier.vo.SupplierRoomType;
import com.trip.supplier.vo.SupplierStay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StayManager {

    private final StayRepository stayRepository;
    private final RoomTypeManager roomTypeManager;

    @Transactional(readOnly = true)
    public List<RoomOption> findActiveRooms(Guests guests) {
        List<Stay> stays = stayRepository.findAllByActiveTrue();
        if (stays.isEmpty()) {
            return List.of();
        }
        Map<UUID, Stay> stayById = stays.stream().collect(Collectors.toMap(Stay::getId, Function.identity()));

        return roomTypeManager.findByStayIds(stayById.keySet(), guests.total()).stream()
                .map(roomType -> RoomOption.from(stayById.get(roomType.getStayId()), roomType))
                .toList();
    }

    @Transactional
    public void sync(Supplier supplier, List<SupplierStay> supplierStays) {

        Map<String, Stay> existingByCode = stayRepository.findAllBySupplier(supplier).stream()
                .collect(Collectors.toMap(Stay::getStayCode, Function.identity()));

        List<Stay> created = new ArrayList<>();
        Map<Stay, List<SupplierRoomType>> supplierRoomTypesByStay = new HashMap<>();
        Set<String> supplierStayCodes = new HashSet<>();
        int updated = 0;

        for (SupplierStay supplierStay : supplierStays) {
            if (!supplierStayCodes.add(supplierStay.stayCode())) {
                continue;
            }
            Stay stay = existingByCode.get(supplierStay.stayCode());
            if (stay == null) {
                stay = Stay.builder()
                        .supplier(supplier)
                        .stayCode(supplierStay.stayCode())
                        .name(supplierStay.name())
                        .build();
                created.add(stay);
            } else if (stay.applyLatestInfo(supplierStay.name())) {
                updated++;
            }
            supplierRoomTypesByStay.put(stay, supplierStay.roomTypes());
        }

        int deactivated = 0;
        for (Stay stay : existingByCode.values()) {
            if (!supplierStayCodes.contains(stay.getStayCode()) && stay.isActive()) {
                stay.markAsNonExist();
                deactivated++;
            }
        }

        stayRepository.saveAll(created);

        Map<UUID, List<SupplierRoomType>> supplierRoomTypesByStayId = new HashMap<>();
        supplierRoomTypesByStay.forEach((stay, roomTypes) -> supplierRoomTypesByStayId.put(stay.getId(), roomTypes));
        roomTypeManager.sync(supplierRoomTypesByStayId);

        log.info("[숙소 동기화 : 완료]: supplier={} | created={} | updated={} | deactivated={}",
                supplier, created.size(), updated, deactivated);
    }
}
