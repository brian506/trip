package com.trip.stay.business;

import com.trip.stay.controller.response.StaySearchResponse;
import com.trip.stay.implement.StayManager;
import com.trip.stay.vo.RoomKey;
import com.trip.stay.vo.RoomOption;
import com.trip.stay.vo.SearchedRoom;
import com.trip.supplier.Supplier;
import com.trip.supplier.global.SupplierDispatcher;
import com.trip.supplier.vo.SupplierDispatchResult;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaySearchService {

    private final StayManager stayManager;
    private final SupplierDispatcher supplierDispatcher;

    public StaySearchResponse search(StayPeriod period, Guests guests) {
        // 이 한 번의 조회가 공급사에 보낼 숙소 코드와, 응답을 내부 식별자로 되돌릴 번역 맵을 같이 준다.
        List<RoomOption> options = stayManager.findActiveRooms(guests);
        if (options.isEmpty()) {
            return new StaySearchResponse(List.of(), List.of());
        }

        SupplierDispatchResult result = supplierDispatcher.dispatch(period, guests, toStayCodes(options));
        if (result.allFailed()) {
            throw new AppException(ErrorType.SUPPLIER_ALL_FAILED, result.failures());
        }
        return new StaySearchResponse(toSearchedRooms(options, result.rooms()), result.failures());
    }

    private Map<Supplier, Set<String>> toStayCodes(List<RoomOption> options) {
        return options.stream().collect(Collectors.groupingBy(
                RoomOption::supplier,
                Collectors.mapping(RoomOption::stayCode, Collectors.toSet())));
    }

    private List<SearchedRoom> toSearchedRooms(List<RoomOption> options, List<SupplierRoom> supplierRooms) {
        Map<RoomKey, RoomOption> optionByKey = options.stream()
                .collect(Collectors.toMap(RoomKey::from, Function.identity(), (first, next) -> first));

        List<SearchedRoom> rooms = new ArrayList<>();
        for (SupplierRoom supplierRoom : supplierRooms) {
            RoomOption option = optionByKey.get(RoomKey.from(supplierRoom));
            if (option == null) {
                // 매핑에 없는 객실은 응답에서 빼고 로그만 남긴다. 검색 경로에는 쓰기를 두지 않는다.
                log.warn("[숙소 검색 : 매핑 없는 객실]: supplier={} | stayCode={} | roomTypeCode={}",
                        supplierRoom.supplier(), supplierRoom.stayCode(), supplierRoom.roomTypeCode());
                continue;
            }
            rooms.add(SearchedRoom.of(option, supplierRoom));
        }
        return rooms;
    }
}
