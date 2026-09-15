package com.trip.stay.implement.dto.b;

import java.util.List;

public record BProperty(String propertyId, String propertyName, List<BRoom> rooms) {
}
