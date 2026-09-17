package com.trip.stay.fixture;

import com.trip.stay.dataaccess.entity.RoomType;
import com.trip.stay.dataaccess.entity.Stay;
import com.trip.supplier.Supplier;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public final class StayEntityFixture {

    private StayEntityFixture() {
    }

    // 식별자는 @UuidGenerator가 채우므로 빌더에 없다. 단위 테스트에는 영속화가 없어 직접 넣는다.
    public static Stay stay(Supplier supplier, String stayCode) {
        Stay stay = Stay.builder()
                .supplier(supplier)
                .stayCode(stayCode)
                .name(stayCode + " 숙소")
                .build();
        ReflectionTestUtils.setField(stay, "id", UUID.randomUUID());
        return stay;
    }

    public static RoomType roomType(Stay stay, String roomTypeCode, int maxOccupancy) {
        RoomType roomType = RoomType.builder()
                .stayId(stay.getId())
                .roomTypeCode(roomTypeCode)
                .name(roomTypeCode + " 객실")
                .maxOccupancy(maxOccupancy)
                .build();
        ReflectionTestUtils.setField(roomType, "id", UUID.randomUUID());
        return roomType;
    }
}
