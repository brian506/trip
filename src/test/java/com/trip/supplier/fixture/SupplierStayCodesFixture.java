package com.trip.supplier.fixture;

import java.util.LinkedHashSet;
import java.util.Set;

public final class SupplierStayCodesFixture {

    private SupplierStayCodesFixture() {
    }

    public static Set<String> stayCodes(int count) {
        Set<String> stayCodes = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            stayCodes.add("H-%04d".formatted(i));
        }
        return stayCodes;
    }
}
