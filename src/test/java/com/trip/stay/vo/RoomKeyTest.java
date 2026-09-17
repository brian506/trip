package com.trip.stay.vo;

import com.trip.stay.fixture.RoomOptionFixture;
import com.trip.supplier.Supplier;
import com.trip.supplier.fixture.SupplierRoomFixture;
import com.trip.supplier.vo.SupplierRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomKeyTest {

    @Test
    @DisplayName("같은 숙소·객실을 가리키는 후보와 공급사 응답은 같은 열쇠가 된다")
    void sameKeyForMatchingOptionAndSupplierRoom() {
        // given
        RoomOption option = RoomOptionFixture.optionA();
        SupplierRoom room = SupplierRoomFixture.roomFor(option);

        // when
        RoomKey fromOption = RoomKey.from(option);
        RoomKey fromRoom = RoomKey.from(room);

        // then
        assertThat(fromOption).isEqualTo(fromRoom);
    }

    @Test
    @DisplayName("숙소 코드와 객실 코드가 같아도 공급사가 다르면 다른 열쇠가 된다")
    void differentKeyWhenSupplierDiffers() {
        // given
        RoomOption option = RoomOptionFixture.option(Supplier.A, "A-1001", "A-R01");
        SupplierRoom otherSupplierRoom = SupplierRoomFixture.room(Supplier.B, "A-1001", "A-R01");

        // when
        RoomKey fromOption = RoomKey.from(option);
        RoomKey fromRoom = RoomKey.from(otherSupplierRoom);

        // then
        assertThat(fromOption).isNotEqualTo(fromRoom);
    }
}
