package com.trip.supplier.vo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record SupplierStayCodes(Set<String> values) {

    public static final int MAX_SIZE = 50;

    public SupplierStayCodes {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("숙소 코드 묶음이 비었습니다.");
        }
        if (values.size() > MAX_SIZE) {
            throw new IllegalArgumentException("숙소 코드는 한 번에 " + MAX_SIZE + "개까지입니다. (" + values.size() + ")");
        }
        values = new LinkedHashSet<>(values);
    }

    public static List<SupplierStayCodes> partition(Set<String> stayCodes) {
        if (stayCodes == null || stayCodes.isEmpty()) {
            return List.of();
        }
        List<SupplierStayCodes> batches = new ArrayList<>();
        Set<String> batch = new LinkedHashSet<>();
        for (String stayCode : stayCodes) {
            batch.add(stayCode);
            if (batch.size() == MAX_SIZE) {
                batches.add(new SupplierStayCodes(batch));
                batch = new LinkedHashSet<>();
            }
        }
        if (!batch.isEmpty()) {
            batches.add(new SupplierStayCodes(batch));
        }
        return List.copyOf(batches);
    }

    public String joined() {
        return String.join(",", values);
    }
}
