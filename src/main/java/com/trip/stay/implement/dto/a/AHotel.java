package com.trip.stay.implement.dto.a;

import java.util.List;

public record AHotel(String hotelCode, String hotelName, List<ARoomType> roomTypes) {
}
