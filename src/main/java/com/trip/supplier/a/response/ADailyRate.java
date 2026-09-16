package com.trip.supplier.a.response;

import java.time.LocalDate;

public record ADailyRate(LocalDate date, Integer remainingRooms, Long nightlyRate, Long taxAmount) {

    public boolean hasAmount() {
        return remainingRooms != null && nightlyRate != null && taxAmount != null;
    }

    public long getAmount() {
        return nightlyRate + taxAmount;
    }
}
