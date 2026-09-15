package com.trip.stay.implement;

import com.trip.stay.dataaccess.entity.RoomType;
import com.trip.stay.dataaccess.repository.RoomTypeRepository;
import com.trip.external.supplier.SupplierRoomType;
import java.util.ArrayList;
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
public class RoomTypeManager {

    private final RoomTypeRepository roomTypeRepository;

    @Transactional
    public void sync(Map<UUID, List<SupplierRoomType>> supplierRoomTypesByStayId) {
        if (supplierRoomTypesByStayId.isEmpty()) {
            return;
        }
        Map<UUID, Map<String, RoomType>> existingByStayId = roomTypeRepository.findAllByStayIdIn(supplierRoomTypesByStayId.keySet())
                .stream()
                .collect(Collectors.groupingBy(
                        RoomType::getStayId,
                        Collectors.toMap(RoomType::getRoomTypeCode, Function.identity())
                ));

        List<RoomType> created = new ArrayList<>();
        int updated = 0;
        int deactivated = 0;

        for (Map.Entry<UUID, List<SupplierRoomType>> entry : supplierRoomTypesByStayId.entrySet()) {
            UUID stayId = entry.getKey();
            Map<String, RoomType> existing = existingByStayId.getOrDefault(stayId, Map.of());
            Set<String> supplierRoomTypeCodes = new HashSet<>();

            for (SupplierRoomType supplierRoomType : entry.getValue()) {
                if (!supplierRoomTypeCodes.add(supplierRoomType.roomTypeCode())) {
                    continue;
                }

                RoomType roomType = existing.get(supplierRoomType.roomTypeCode());
                if (roomType == null) {
                    created.add(RoomType.builder()
                            .stayId(stayId)
                            .roomTypeCode(supplierRoomType.roomTypeCode())
                            .name(supplierRoomType.name())
                            .maxOccupancy(supplierRoomType.maxOccupancy())
                            .build());
                } else if (roomType.applyLatestInfo(supplierRoomType.name(), supplierRoomType.maxOccupancy())) {
                    updated++;
                }
            }

            for (RoomType roomType : existing.values()) {
                if (!supplierRoomTypeCodes.contains(roomType.getRoomTypeCode()) && roomType.isActive()) {
                    roomType.markAsNonExist();
                    deactivated++;
                }
            }
        }

        roomTypeRepository.saveAll(created);
        log.info("[객실 타입 동기화 : 완료]: created={} | updated={} | deactivated={}", created.size(), updated, deactivated);
    }
}
