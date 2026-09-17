package com.trip.stay.vo;

import com.trip.stay.fixture.RoomOptionFixture;
import com.trip.supplier.vo.SupplierRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.trip.supplier.fixture.SupplierRoomFixture.roomFor;
import static org.assertj.core.api.Assertions.assertThat;

class SearchedRoomTest {

    @Test
    @DisplayName("숙소·객실 정보는 후보에서, 예약 가능 수·조식·통화·총액은 공급사 응답에서 가져온다")
    void takeStayInfoFromOptionAndPricingFromSupplierRoom() {
        // given
        RoomOption option = RoomOptionFixture.optionA();
        SupplierRoom room = roomFor(option, 4, 275_000L);

        // when
        SearchedRoom searchedRoom = SearchedRoom.of(option, room);

        // then
        assertThat(searchedRoom.stayId()).isEqualTo(option.stayId());
        assertThat(searchedRoom.stayName()).isEqualTo(option.stayName());
        assertThat(searchedRoom.roomTypeId()).isEqualTo(option.roomTypeId());
        assertThat(searchedRoom.roomTypeName()).isEqualTo(option.roomTypeName());
        assertThat(searchedRoom.maxOccupancy()).isEqualTo(option.maxOccupancy());
        assertThat(searchedRoom.supplier()).isEqualTo(option.supplier());

        assertThat(searchedRoom.availableRooms()).isEqualTo(4);
        assertThat(searchedRoom.totalPrice()).isEqualTo(275_000L);
        assertThat(searchedRoom.breakfastIncluded()).isEqualTo(room.breakfastIncluded());
        assertThat(searchedRoom.currency()).isEqualTo(room.currency());
    }
}
