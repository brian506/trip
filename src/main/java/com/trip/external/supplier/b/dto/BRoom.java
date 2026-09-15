package com.trip.external.supplier.b.dto;

// roomId는 이름과 달리 객실 타입 식별자다. 개별 물리 객실이 아니다.
public record BRoom(String roomId, String roomName, Integer maxOccupancy) {
}
