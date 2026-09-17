package com.trip.supplier.fixture;

import com.trip.support.fixture.StayPeriodFixture;

public final class SupplierResponseFixture {

    public static final String A_STAY_CODE = "A-1001";
    public static final String A_STAY_NAME = "에이 호텔";
    public static final String A_ROOM_TYPE_CODE = "A-R01";
    public static final String A_ROOM_TYPE_NAME = "디럭스 트윈";
    public static final int A_MAX_OCCUPANCY = 3;
    public static final String A_CURRENCY = "KRW";
    public static final boolean A_BREAKFAST_INCLUDED = false;
    public static final long A_TOTAL_PRICE = 275_000L;
    public static final int A_AVAILABLE_ROOMS = 3;

    public static final String B_STAY_CODE = "B-2001";
    public static final String B_STAY_NAME = "비 프로퍼티";
    public static final String B_ROOM_TYPE_CODE = "B-R01";
    public static final String B_ROOM_TYPE_NAME = "스탠다드 더블";
    public static final int B_MAX_OCCUPANCY = 2;
    public static final String B_CURRENCY = "KRW";
    public static final boolean B_BREAKFAST_INCLUDED = true;
    public static final long B_TOTAL_PRICE = 452_000L;
    public static final int B_AVAILABLE_ROOMS = 4;

    private SupplierResponseFixture() {
    }

    public static String aStays(String... hotels) {
        return "{\"items\":[%s]}".formatted(String.join(",", hotels));
    }

    public static String aHotel() {
        return aHotel(A_STAY_CODE, A_STAY_NAME, aRoomType());
    }

    public static String aHotel(String stayCode, String stayName, String... roomTypes) {
        return """
                {"hotelCode":"%s","hotelName":"%s","roomTypes":[%s]}"""
                .formatted(stayCode, stayName, String.join(",", roomTypes));
    }

    public static String aRoomType() {
        return aRoomType(A_ROOM_TYPE_CODE, A_ROOM_TYPE_NAME, A_MAX_OCCUPANCY);
    }

    public static String aRoomType(String roomTypeCode, String roomTypeName, int maxOccupancy) {
        return """
                {"roomTypeCode":"%s","roomTypeName":"%s","maxOccupancy":%d}"""
                .formatted(roomTypeCode, roomTypeName, maxOccupancy);
    }

    public static String aRooms(String... items) {
        return "{\"items\":[%s]}".formatted(String.join(",", items));
    }

    public static String aRoom() {
        return aRoom(A_STAY_CODE, A_ROOM_TYPE_CODE);
    }

    public static String aRoom(String stayCode, String roomTypeCode) {
        return """
                {"hotelCode":"%s","roomTypeCode":"%s","breakfastIncluded":%b,"currency":"%s","dailyRates":[
                {"date":"%s","remainingRooms":5,"nightlyRate":120000,"taxAmount":12000},
                {"date":"%s","remainingRooms":3,"nightlyRate":130000,"taxAmount":13000}]}"""
                .formatted(stayCode, roomTypeCode, A_BREAKFAST_INCLUDED, A_CURRENCY,
                        StayPeriodFixture.day(0), StayPeriodFixture.day(1));
    }

    public static String aError(String code) {
        return "{\"error\":\"%s\"}".formatted(code);
    }

    public static String bStays(String... properties) {
        return """
                {"resultCode":"0000","resultMessage":"OK","data":{"items":[%s]}}"""
                .formatted(String.join(",", properties));
    }

    public static String bProperty() {
        return bProperty(B_STAY_CODE, B_STAY_NAME, bRoomType());
    }

    public static String bProperty(String stayCode, String stayName, String... roomTypes) {
        return """
                {"propertyId":"%s","propertyName":"%s","rooms":[%s]}"""
                .formatted(stayCode, stayName, String.join(",", roomTypes));
    }

    public static String bRoomType() {
        return bRoomType(B_ROOM_TYPE_CODE, B_ROOM_TYPE_NAME, B_MAX_OCCUPANCY);
    }

    public static String bRoomType(String roomTypeCode, String roomTypeName, int maxOccupancy) {
        return """
                {"roomId":"%s","roomName":"%s","maxOccupancy":%d}"""
                .formatted(roomTypeCode, roomTypeName, maxOccupancy);
    }

    public static String bRooms(String... items) {
        return """
                {"resultCode":"0000","resultMessage":"OK","data":{"items":[%s]}}"""
                .formatted(String.join(",", items));
    }

    public static String bRoom() {
        return bRoom(B_STAY_CODE, B_ROOM_TYPE_CODE);
    }

    public static String bRoom(String stayCode, String roomTypeCode) {
        return """
                {"propertyId":"%s","roomId":"%s","breakfastIncluded":%b,"currency":"%s","totalPrice":%d,"inventory":[
                {"date":"%s","remainingRooms":7},
                {"date":"%s","remainingRooms":4}]}"""
                .formatted(stayCode, roomTypeCode, B_BREAKFAST_INCLUDED, B_CURRENCY, B_TOTAL_PRICE,
                        StayPeriodFixture.day(0), StayPeriodFixture.day(1));
    }
}
