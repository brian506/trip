package com.trip.supplier.fixture;

import com.trip.supplier.a.response.AAvailabilityItem;
import com.trip.supplier.a.response.AAvailabilityResponse;
import com.trip.supplier.a.response.ADailyRate;
import com.trip.support.fixture.StayPeriodFixture;

import java.util.Arrays;
import java.util.List;

public final class AResponseFixture {

    public static final String STAY_CODE = "A-1001";
    public static final String ROOM_TYPE_CODE = "A-R01";
    public static final String OTHER_ROOM_TYPE_CODE = "A-R02";
    public static final String CURRENCY = "KRW";
    public static final boolean BREAKFAST_INCLUDED = false;

    private AResponseFixture() {
    }

    public static AAvailabilityResponse response(AAvailabilityItem... items) {
        return new AAvailabilityResponse(Arrays.asList(items));
    }

    public static AAvailabilityItem item() {
        return item(ROOM_TYPE_CODE, fullRates());
    }

    public static AAvailabilityItem item(String roomTypeCode, List<ADailyRate> dailyRates) {
        return item(roomTypeCode, BREAKFAST_INCLUDED, CURRENCY, dailyRates);
    }

    public static AAvailabilityItem item(String roomTypeCode, boolean breakfastIncluded,
                                         String currency, List<ADailyRate> dailyRates) {
        return new AAvailabilityItem(
                STAY_CODE,
                "에이 호텔",
                roomTypeCode,
                "디럭스 트윈",
                3,
                breakfastIncluded,
                currency,
                dailyRates
        );
    }

    public static List<ADailyRate> fullRates() {
        return List.of(
                rate(0, 5, 120_000L, 12_000L),
                rate(1, 3, 130_000L, 13_000L)
        );
    }

    public static ADailyRate rate(int dayIndex, int remainingRooms, long nightlyRate, long taxAmount) {
        return new ADailyRate(StayPeriodFixture.day(dayIndex), remainingRooms, nightlyRate, taxAmount);
    }
}
