package com.trip.supplier.fixture;

import com.trip.supplier.b.response.BInventory;
import com.trip.supplier.b.response.BSearchData;
import com.trip.supplier.b.response.BSearchItem;
import com.trip.support.fixture.StayPeriodFixture;

import java.util.Arrays;
import java.util.List;

public final class BResponseFixture {

    public static final String STAY_CODE = "B-2001";
    public static final String ROOM_TYPE_CODE = "B-R01";
    public static final String OTHER_ROOM_TYPE_CODE = "B-R02";
    public static final String CURRENCY = "KRW";
    public static final boolean BREAKFAST_INCLUDED = true;
    public static final long TOTAL_PRICE = 452_000L;

    private BResponseFixture() {
    }

    public static BSearchData data(BSearchItem... items) {
        return new BSearchData(Arrays.asList(items));
    }

    public static BSearchItem item() {
        return item(ROOM_TYPE_CODE, TOTAL_PRICE, fullInventory());
    }

    public static BSearchItem item(String roomTypeCode, long totalPrice, List<BInventory> inventory) {
        return new BSearchItem(
                STAY_CODE,
                "비 프로퍼티",
                roomTypeCode,
                "스탠다드 더블",
                2,
                BREAKFAST_INCLUDED,
                CURRENCY,
                totalPrice,
                true,
                inventory
        );
    }

    public static List<BInventory> fullInventory() {
        return List.of(
                inventory(0, 7),
                inventory(1, 4)
        );
    }

    public static BInventory inventory(int dayIndex, int remainingRooms) {
        return new BInventory(StayPeriodFixture.day(dayIndex), remainingRooms);
    }
}
