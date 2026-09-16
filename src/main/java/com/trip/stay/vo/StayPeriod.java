package com.trip.stay.vo;

import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 숙박 기간
public record StayPeriod(LocalDate checkIn, LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;

    public StayPeriod {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new AppException(ErrorType.INVALID_STAY_PERIOD);
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new AppException(ErrorType.PAST_CHECK_IN);
        }
        //  체크아웃일은 숙박일에 포함x
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw new AppException(ErrorType.STAY_PERIOD_TOO_LONG);
        }
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
