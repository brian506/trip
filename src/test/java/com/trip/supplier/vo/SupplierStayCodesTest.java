package com.trip.supplier.vo;

import com.trip.supplier.fixture.SupplierStayCodesFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierStayCodesTest {

    @Test
    @DisplayName("숙소 코드를 50개씩 나눠 묶음 목록을 만든다")
    void partitionIntoBatchesOfFifty() {
        // given
        Set<String> stayCodes = SupplierStayCodesFixture.stayCodes(120);

        // when
        List<SupplierStayCodes> batches = SupplierStayCodes.partition(stayCodes);

        // then
        assertThat(batches).hasSize(3);
        assertThat(batches)
                .extracting(batch -> batch.values().size())
                .containsExactly(50, 50, 20);
        assertThat(batches)
                .flatExtracting(SupplierStayCodes::values)
                .containsExactlyInAnyOrderElementsOf(stayCodes);
    }

    @Test
    @DisplayName("코드가 정확히 50개면 묶음은 1개다")
    void singleBatchForExactlyFiftyCodes() {
        // given
        Set<String> stayCodes = SupplierStayCodesFixture.stayCodes(50);

        // when
        List<SupplierStayCodes> batches = SupplierStayCodes.partition(stayCodes);

        // then
        assertThat(batches).hasSize(1);
        assertThat(batches.getFirst().values()).hasSize(50);
    }

    @Test
    @DisplayName("코드가 51개면 50개짜리와 1개짜리 묶음 2개가 된다")
    void splitIntoTwoBatchesForFiftyOneCodes() {
        // given
        Set<String> stayCodes = SupplierStayCodesFixture.stayCodes(51);

        // when
        List<SupplierStayCodes> batches = SupplierStayCodes.partition(stayCodes);

        // then
        assertThat(batches)
                .extracting(batch -> batch.values().size())
                .containsExactly(50, 1);
    }
}
