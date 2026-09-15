package com.trip.external.supplier.a.dto;

import java.util.List;

public record AHotel(String hotelCode, String hotelName, List<ARoomType> roomTypes) {
}
