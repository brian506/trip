package com.trip.support.fixture;

import com.trip.support.vo.StayPeriod;

import java.time.LocalDate;

public final class StayPeriodFixture {

    public static final LocalDate CHECK_IN = LocalDate.now().plusDays(7);

    private StayPeriodFixture() {
    }

    public static StayPeriod twoNights() {
        return nights(2);
    }

    public static StayPeriod nights(int nights) {
        return new StayPeriod(CHECK_IN, CHECK_IN.plusDays(nights));
    }

    public static LocalDate day(int index) {
        return CHECK_IN.plusDays(index);
    }
}
