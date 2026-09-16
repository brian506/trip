package com.trip.stay.vo;

import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;


public record Guests(Integer adults, Integer children) {

    // 성인은 1명이상, 아동은 기본값 0
    public Guests {
        if (children == null) {
            children = 0;
        }
        if (adults == null || adults < 1 || children < 0) {
            throw new AppException(ErrorType.INVALID_GUESTS);
        }
    }

    public int total() {
        return adults + children;
    }
}
