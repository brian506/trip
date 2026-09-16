package com.trip.supplier.a.response;

import com.trip.supplier.Supplier;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.vo.StayPeriod;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AAvailabilityItem(
        String hotelCode,
        String hotelName,
        String roomTypeCode,
        String roomTypeName,
        Integer maxOccupancy,
        Boolean breakfastIncluded,
        String currency,
        List<ADailyRate> dailyRates
) {

    public SupplierRoom toSupplierRoom(StayPeriod period) {
        if (dailyRates == null) {
            return null;
        }
        Map<LocalDate, ADailyRate> rateByDate = new HashMap<>();
        for (ADailyRate dailyRate : dailyRates) {
            if (dailyRate != null && dailyRate.date() != null) {
                rateByDate.putIfAbsent(dailyRate.date(), dailyRate);
            }
        }
        long totalPrice = 0;
        int availableRooms = Integer.MAX_VALUE;
        for (LocalDate date : period.dates()) {
            ADailyRate dailyRate = rateByDate.get(date);
            if (dailyRate == null || !dailyRate.hasAmount()) {
                return null;
            }
            totalPrice += dailyRate.getAmount();
            availableRooms = Math.min(availableRooms, dailyRate.remainingRooms());
        }
        return new SupplierRoom(Supplier.A, hotelCode, roomTypeCode, availableRooms,
                Boolean.TRUE.equals(breakfastIncluded), currency, totalPrice);
    }
}
