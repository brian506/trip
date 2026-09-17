package com.trip.supplier;

import com.trip.support.exception.supplier.SupplierFailureType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierFailureTypeTest {

    @Test
    @DisplayName("UNAVAILABLE·INTERNAL만 공급사 장애로 보고 나머지는 재시도·브레이커 대상이 아니다")
    void onlyOutageTypesIndicateSupplierDown() {
        // given
        SupplierFailureType[] types = SupplierFailureType.values();

        // when
        List<SupplierFailureType> down = Arrays.stream(types)
                .filter(SupplierFailureType::indicatesSupplierDown)
                .toList();

        // then
        assertThat(down).containsExactlyInAnyOrder(
                SupplierFailureType.UNAVAILABLE,
                SupplierFailureType.INTERNAL
        );
    }
}
