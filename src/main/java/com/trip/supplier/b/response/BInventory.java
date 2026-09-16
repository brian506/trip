package com.trip.supplier.b.response;

import java.time.LocalDate;

public record BInventory(LocalDate date, Integer remainingRooms) {
}
