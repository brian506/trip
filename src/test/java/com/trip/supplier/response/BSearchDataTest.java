package com.trip.supplier.response;

import com.trip.supplier.Supplier;
import com.trip.supplier.b.response.BSearchData;
import com.trip.supplier.fixture.BResponseFixture;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.fixture.StayPeriodFixture;
import com.trip.support.vo.StayPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.trip.supplier.fixture.BResponseFixture.OTHER_ROOM_TYPE_CODE;
import static com.trip.supplier.fixture.BResponseFixture.ROOM_TYPE_CODE;
import static com.trip.supplier.fixture.BResponseFixture.STAY_CODE;
import static com.trip.supplier.fixture.BResponseFixture.TOTAL_PRICE;
import static com.trip.supplier.fixture.BResponseFixture.data;
import static com.trip.supplier.fixture.BResponseFixture.inventory;
import static com.trip.supplier.fixture.BResponseFixture.item;
import static org.assertj.core.api.Assertions.assertThat;

class BSearchDataTest {

    @Test
    @DisplayName("B의 총액은 응답의 totalPrice를 그대로 쓴다")
    void useTotalPriceAsGiven() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        BSearchData searchData = data(item());

        // when
        List<SupplierRoom> rooms = searchData.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().totalPrice()).isEqualTo(TOTAL_PRICE);
    }

    @Test
    @DisplayName("B의 예약 가능 수는 기간 날짜별 remainingRooms의 최솟값이다")
    void takeMinimumRemainingRoomsOverPeriod() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        BSearchData searchData = data(item(ROOM_TYPE_CODE, TOTAL_PRICE, List.of(
                inventory(0, 4),
                inventory(1, 7)
        )));

        // when
        List<SupplierRoom> rooms = searchData.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().availableRooms()).isEqualTo(4);
    }

    @Test
    @DisplayName("B의 총액은 숙박일 수가 달라도 totalPrice 그대로다")
    void keepTotalPriceRegardlessOfNights() {
        // given
        StayPeriod threeNights = StayPeriodFixture.nights(3);
        BSearchData searchData = data(item(ROOM_TYPE_CODE, TOTAL_PRICE, List.of(
                inventory(0, 7),
                inventory(1, 4),
                inventory(2, 6)
        )));

        // when
        List<SupplierRoom> rooms = searchData.toSupplierRooms(threeNights);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().totalPrice()).isEqualTo(TOTAL_PRICE);
    }

    @Test
    @DisplayName("기간 중 하루라도 remainingRooms가 0이면 예약 가능 수는 0이다")
    void takeZeroWhenAnyNightIsSoldOut() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        BSearchData searchData = data(item(ROOM_TYPE_CODE, TOTAL_PRICE, List.of(
                inventory(0, 7),
                inventory(1, 0)
        )));

        // when
        List<SupplierRoom> rooms = searchData.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().availableRooms()).isZero();
    }

    @Test
    @DisplayName("요청 기간의 날짜가 응답에 하나라도 빠지면 그 항목을 버린다")
    void dropItemMissingAnyDateOfPeriod() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        BSearchData searchData = data(item(ROOM_TYPE_CODE, TOTAL_PRICE, List.of(
                inventory(0, 7)
        )));

        // when
        List<SupplierRoom> rooms = searchData.toSupplierRooms(period);

        // then
        assertThat(rooms).isEmpty();
    }

    @Test
    @DisplayName("항목 하나를 버려도 같은 응답의 나머지 항목은 그대로 돌려준다")
    void keepRemainingItemsWhenOneIsDropped() {
        // given
        StayPeriod period = StayPeriodFixture.twoNights();
        BSearchData searchData = data(
                item(ROOM_TYPE_CODE, TOTAL_PRICE, List.of(inventory(0, 7))),
                item(OTHER_ROOM_TYPE_CODE, 389_000L, BResponseFixture.fullInventory())
        );

        // when
        List<SupplierRoom> rooms = searchData.toSupplierRooms(period);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().supplier()).isEqualTo(Supplier.B);
        assertThat(rooms.getFirst().stayCode()).isEqualTo(STAY_CODE);
        assertThat(rooms.getFirst().roomTypeCode()).isEqualTo(OTHER_ROOM_TYPE_CODE);
        assertThat(rooms.getFirst().totalPrice()).isEqualTo(389_000L);
        assertThat(rooms.getFirst().availableRooms()).isEqualTo(4);
    }
}
