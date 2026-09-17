package com.trip.stay.implement;

import com.trip.stay.dataaccess.repository.RoomTypeRepository;
import com.trip.stay.dataaccess.repository.StayRepository;
import com.trip.stay.fixture.RoomOptionFixture;
import com.trip.stay.vo.RoomOption;
import com.trip.stay.vo.SearchedRoom;
import com.trip.supplier.Supplier;
import com.trip.supplier.fixture.SupplierRoomFixture;
import com.trip.supplier.vo.SupplierRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(MockitoExtension.class)
class StayManagerTest {

    @Mock
    private StayRepository stayRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    private StayManager stayManager;

    @BeforeEach
    void setUp() {
        stayManager = new StayManager(stayRepository, new RoomTypeManager(roomTypeRepository));
    }

    @Test
    @DisplayName("후보와 공급사 응답을 공급사·숙소 코드·객실 코드로 짝지어 검색 결과를 만든다")
    void matchOptionsWithSupplierRoomsBySupplierAndCodes() {
        // given
        RoomOption optionA = RoomOptionFixture.optionA();
        RoomOption optionB = RoomOptionFixture.optionB();
        SupplierRoom roomA = SupplierRoomFixture.roomFor(optionA, 4, 275_000L);
        SupplierRoom roomB = SupplierRoomFixture.roomFor(optionB, 6, 452_000L);

        // when
        List<SearchedRoom> searchedRooms =
                stayManager.toSearchedRooms(List.of(optionA, optionB), List.of(roomB, roomA));

        // then
        assertThat(searchedRooms).hasSize(2);
        assertThat(searchedRooms)
                .extracting(SearchedRoom::roomTypeId, SearchedRoom::availableRooms, SearchedRoom::totalPrice)
                .containsExactlyInAnyOrder(
                        tuple(optionA.roomTypeId(), 4, 275_000L),
                        tuple(optionB.roomTypeId(), 6, 452_000L)
                );
    }

    @Test
    @DisplayName("매핑에 없는 객실 코드가 응답에 오면 그 항목은 결과에서 뺀다")
    void dropSupplierRoomWithUnmappedRoomTypeCode() {
        // given
        RoomOption optionA = RoomOptionFixture.optionA();
        SupplierRoom mapped = SupplierRoomFixture.roomFor(optionA);
        SupplierRoom unmapped = SupplierRoomFixture.room(Supplier.A, optionA.stayCode(), "A-R99");

        // when
        List<SearchedRoom> searchedRooms =
                stayManager.toSearchedRooms(List.of(optionA), List.of(mapped, unmapped));

        // then
        assertThat(searchedRooms).hasSize(1);
        assertThat(searchedRooms.getFirst().roomTypeId()).isEqualTo(optionA.roomTypeId());
    }

    @Test
    @DisplayName("숙소·객실 코드가 같아도 공급사가 다른 응답은 그 후보와 짝지어지지 않는다")
    void doNotMatchAcrossSuppliers() {
        // given
        RoomOption optionA = RoomOptionFixture.option(Supplier.A, "A-1001", "A-R01");
        SupplierRoom fromOtherSupplier = SupplierRoomFixture.room(Supplier.B, "A-1001", "A-R01");

        // when
        List<SearchedRoom> searchedRooms =
                stayManager.toSearchedRooms(List.of(optionA), List.of(fromOtherSupplier));

        // then
        assertThat(searchedRooms).isEmpty();
    }

    @Test
    @DisplayName("후보에 있지만 공급사 응답에 없는 객실은 결과에 나오지 않는다")
    void dropOptionWithoutSupplierRoom() {
        // given
        RoomOption answered = RoomOptionFixture.optionA();
        RoomOption unanswered = RoomOptionFixture.anotherRoomOf(answered, "A-R02");
        SupplierRoom room = SupplierRoomFixture.roomFor(answered);

        // when
        List<SearchedRoom> searchedRooms =
                stayManager.toSearchedRooms(List.of(answered, unanswered), List.of(room));

        // then
        assertThat(searchedRooms).hasSize(1);
        assertThat(searchedRooms.getFirst().roomTypeId()).isEqualTo(answered.roomTypeId());
    }

    @Test
    @DisplayName("후보를 공급사별로 묶어 각 공급사에 물어볼 숙소 코드 집합을 만든다")
    void groupStayCodesBySupplier() {
        // given
        RoomOption optionA = RoomOptionFixture.optionA();
        RoomOption optionB = RoomOptionFixture.optionB();

        // when
        Map<Supplier, Set<String>> stayCodes = stayManager.toStayCodes(List.of(optionA, optionB));

        // then
        assertThat(stayCodes).containsOnlyKeys(Supplier.A, Supplier.B);
        assertThat(stayCodes.get(Supplier.A)).containsExactly(optionA.stayCode());
        assertThat(stayCodes.get(Supplier.B)).containsExactly(optionB.stayCode());
    }

    @Test
    @DisplayName("같은 숙소의 객실이 여러 건이면 숙소 코드는 한 번만 담긴다")
    void collectStayCodeOnceForMultipleRooms() {
        // given
        RoomOption first = RoomOptionFixture.optionA();
        RoomOption second = RoomOptionFixture.anotherRoomOf(first, "A-R02");

        // when
        Map<Supplier, Set<String>> stayCodes = stayManager.toStayCodes(List.of(first, second));

        // then
        assertThat(stayCodes.get(Supplier.A)).containsExactly(first.stayCode());
    }
}
