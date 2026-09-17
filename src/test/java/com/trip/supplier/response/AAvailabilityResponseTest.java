package com.trip.supplier.response;

import com.trip.supplier.Supplier;
import com.trip.supplier.a.response.AAvailabilityResponse;
import com.trip.supplier.fixture.AResponseFixture;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.fixture.StayPeriodFixture;
import com.trip.support.vo.StayPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.trip.supplier.fixture.AResponseFixture.ROOM_TYPE_CODE;
import static com.trip.supplier.fixture.AResponseFixture.OTHER_ROOM_TYPE_CODE;
import static com.trip.supplier.fixture.AResponseFixture.STAY_CODE;
import static com.trip.supplier.fixture.AResponseFixture.item;
import static com.trip.supplier.fixture.AResponseFixture.rate;
import static com.trip.supplier.fixture.AResponseFixture.response;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class AAvailabilityResponseTest {

    @Test
    @DisplayName("A의 총액은 기간 날짜별 nightlyRate와 taxAmount를 모두 더한 값이다")
    void sumNightlyRateAndTaxOverPeriod() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        AAvailabilityResponse response = response(item());

        // when
        List<SupplierRoom> rooms = response.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().totalPrice()).isEqualTo(275_000L);
    }

    @Test
    @DisplayName("A의 예약 가능 수는 기간 날짜별 remainingRooms의 최솟값이다")
    void takeMinimumRemainingRoomsOverPeriod() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        AAvailabilityResponse response = response(item(ROOM_TYPE_CODE, List.of(
                rate(0, 3, 120_000L, 12_000L),
                rate(1, 5, 130_000L, 13_000L)
        )));

        // when
        List<SupplierRoom> rooms = response.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().availableRooms()).isEqualTo(3);
    }

    @Test
    @DisplayName("A는 통화와 조식 포함 여부를 공급사가 준 값 그대로 옮긴다")
    void carryCurrencyAndBreakfastAsGiven() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        AAvailabilityResponse response = response(
                item(ROOM_TYPE_CODE, true, "KRW", AResponseFixture.fullRates()),
                item(OTHER_ROOM_TYPE_CODE, false, "USD", AResponseFixture.fullRates())
        );

        // when
        List<SupplierRoom> rooms = response.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(2);
        assertThat(rooms)
                .extracting(SupplierRoom::roomTypeCode, SupplierRoom::breakfastIncluded, SupplierRoom::currency)
                .containsExactlyInAnyOrder(
                        tuple(ROOM_TYPE_CODE, true, "KRW"),
                        tuple(OTHER_ROOM_TYPE_CODE, false, "USD")
                );
    }

    @Test
    @DisplayName("기간 중 하루라도 remainingRooms가 0이면 예약 가능 수는 0이다")
    void takeZeroWhenAnyNightIsSoldOut() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        AAvailabilityResponse response = response(item(ROOM_TYPE_CODE, List.of(
                rate(0, 5, 120_000L, 12_000L),
                rate(1, 0, 130_000L, 13_000L)
        )));

        // when
        List<SupplierRoom> rooms = response.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().availableRooms()).isZero();
    }

    @Test
    @DisplayName("요청 기간의 날짜가 응답에 하나라도 빠지면 그 항목을 버린다")
    void dropItemMissingAnyDateOfPeriod() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        AAvailabilityResponse response = response(item(ROOM_TYPE_CODE, List.of(
                rate(0, 5, 120_000L, 12_000L)
        )));

        // when
        List<SupplierRoom> rooms = response.toSupplierRooms(period);

        // then
        assertThat(rooms).isEmpty();
    }

    @Test
    @DisplayName("항목 하나를 버려도 같은 응답의 나머지 항목은 그대로 돌려준다")
    void keepRemainingItemsWhenOneIsDropped() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        AAvailabilityResponse response = response(
                item(ROOM_TYPE_CODE, List.of(rate(0, 5, 120_000L, 12_000L))),
                item(OTHER_ROOM_TYPE_CODE, AResponseFixture.fullRates())
        );

        // when
        List<SupplierRoom> rooms = response.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().supplier()).isEqualTo(Supplier.A);
        assertThat(rooms.getFirst().stayCode()).isEqualTo(STAY_CODE);
        assertThat(rooms.getFirst().roomTypeCode()).isEqualTo(OTHER_ROOM_TYPE_CODE);
        assertThat(rooms.getFirst().totalPrice()).isEqualTo(275_000L);
        assertThat(rooms.getFirst().availableRooms()).isEqualTo(3);
    }
}
