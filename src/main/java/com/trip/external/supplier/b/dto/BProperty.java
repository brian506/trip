package com.trip.external.supplier.b.dto;

import java.util.List;

public record BProperty(String propertyId, String propertyName, List<BRoom> rooms) {
}
