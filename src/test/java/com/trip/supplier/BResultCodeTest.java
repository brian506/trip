package com.trip.supplier;

import com.trip.supplier.b.BResultCode;
import com.trip.support.exception.supplier.SupplierFailureType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BResultCodeTest {

    @Test
    @DisplayName("본문 코드 E400·E401·E429·E500·E503을 각각 BAD_REQUEST·AUTH·RATE_LIMITED·INTERNAL·UNAVAILABLE로 가른다")
    void classifyKnownResultCodes() {
        // given
        List<String> resultCodes = List.of("E400", "E401", "E429", "E500", "E503");

        // when
        List<SupplierFailureType> types = resultCodes.stream()
                .map(BResultCode::classify)
                .toList();

        // then
        assertThat(types).containsExactly(
                SupplierFailureType.BAD_REQUEST,
                SupplierFailureType.AUTH,
                SupplierFailureType.RATE_LIMITED,
                SupplierFailureType.INTERNAL,
                SupplierFailureType.UNAVAILABLE
        );
    }

    @Test
    @DisplayName("본문에 코드가 없으면 MALFORMED로 본다")
    void classifyMissingResultCodeAsMalformed() {
        // given
        String missing = null;

        // when
        SupplierFailureType type = BResultCode.classify(missing);

        // then
        assertThat(type).isEqualTo(SupplierFailureType.MALFORMED);
    }

    @Test
    @DisplayName("모르는 코드면 INTERNAL로 본다")
    void classifyUnknownResultCodeAsInternal() {
        // given
        String unknown = "E999";

        // when
        SupplierFailureType type = BResultCode.classify(unknown);

        // then
        assertThat(type).isEqualTo(SupplierFailureType.INTERNAL);
    }
}
