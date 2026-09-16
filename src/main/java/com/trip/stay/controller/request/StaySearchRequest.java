package com.trip.stay.controller.request;

import com.trip.stay.vo.Guests;
import com.trip.stay.vo.StayPeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


public record StaySearchRequest(

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @NotNull(message = "체크인 날짜는 필수입니다")
        LocalDate checkIn,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @NotNull(message = "체크아웃 날짜는 필수입니다")
        LocalDate checkOut,

        @NotNull(message = "성인 수는 필수입니다")
        @Min(value = 1, message = "성인은 1명 이상이어야 합니다")
        Integer adults,

        @Min(value = 0, message = "아동은 0명 이상이어야 합니다")
        Integer children
) {

    public StayPeriod toPeriod() {
        return new StayPeriod(checkIn, checkOut);
    }

    public Guests toGuests() {
        return new Guests(adults, children);
    }
}
