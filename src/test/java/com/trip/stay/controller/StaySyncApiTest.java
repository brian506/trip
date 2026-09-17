package com.trip.stay.controller;

import com.trip.stay.controller.response.StaySearchResponse;
import com.trip.stay.vo.SearchedRoom;
import com.trip.supplier.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.trip.supplier.fixture.SupplierResponseFixture.A_AVAILABLE_ROOMS;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_BREAKFAST_INCLUDED;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_CURRENCY;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_MAX_OCCUPANCY;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_ROOM_TYPE_CODE;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_ROOM_TYPE_NAME;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_STAY_CODE;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_STAY_NAME;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_TOTAL_PRICE;
import static com.trip.supplier.fixture.SupplierResponseFixture.B_AVAILABLE_ROOMS;
import static com.trip.supplier.fixture.SupplierResponseFixture.B_BREAKFAST_INCLUDED;
import static com.trip.supplier.fixture.SupplierResponseFixture.B_CURRENCY;
import static com.trip.supplier.fixture.SupplierResponseFixture.B_MAX_OCCUPANCY;
import static com.trip.supplier.fixture.SupplierResponseFixture.B_ROOM_TYPE_NAME;
import static com.trip.supplier.fixture.SupplierResponseFixture.B_STAY_NAME;
import static com.trip.supplier.fixture.SupplierResponseFixture.B_TOTAL_PRICE;
import static com.trip.supplier.fixture.SupplierResponseFixture.aError;
import static com.trip.supplier.fixture.SupplierResponseFixture.aHotel;
import static com.trip.supplier.fixture.SupplierResponseFixture.aRoom;
import static com.trip.supplier.fixture.SupplierResponseFixture.aRoomType;
import static com.trip.supplier.fixture.SupplierResponseFixture.aRooms;
import static com.trip.supplier.fixture.SupplierResponseFixture.aStays;
import static com.trip.supplier.fixture.SupplierResponseFixture.bProperty;
import static com.trip.supplier.fixture.SupplierResponseFixture.bRoom;
import static com.trip.supplier.fixture.SupplierResponseFixture.bRooms;
import static com.trip.supplier.fixture.SupplierResponseFixture.bStays;
import static com.trip.support.SupplierMockServer.A_ROOMS;
import static com.trip.support.SupplierMockServer.A_STAYS;
import static com.trip.support.SupplierMockServer.B_ROOMS;
import static com.trip.support.SupplierMockServer.B_STAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class StaySyncApiTest extends StayApiTest {

    @Test
    @DisplayName("동기화한 뒤 검색하면 공급사가 준 숙소와 객실이 요금·재고와 함께 나온다")
    void returnSyncedStaysWithSupplierRates() throws Exception {
        // given
        supplierServer.given(A_STAYS, aStays(aHotel()));
        supplierServer.given(B_STAYS, bStays(bProperty()));
        supplierServer.given(A_ROOMS, aRooms(aRoom()));
        supplierServer.given(B_ROOMS, bRooms(bRoom()));
        sync();

        // when
        StaySearchResponse response = searchResult(2);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::supplier, SearchedRoom::stayName, SearchedRoom::roomTypeName,
                        SearchedRoom::maxOccupancy, SearchedRoom::availableRooms,
                        SearchedRoom::breakfastIncluded, SearchedRoom::currency, SearchedRoom::totalPrice)
                .containsExactlyInAnyOrder(
                        tuple(Supplier.A, A_STAY_NAME, A_ROOM_TYPE_NAME, A_MAX_OCCUPANCY, A_AVAILABLE_ROOMS,
                                A_BREAKFAST_INCLUDED, A_CURRENCY, A_TOTAL_PRICE),
                        tuple(Supplier.B, B_STAY_NAME, B_ROOM_TYPE_NAME, B_MAX_OCCUPANCY, B_AVAILABLE_ROOMS,
                                B_BREAKFAST_INCLUDED, B_CURRENCY, B_TOTAL_PRICE));
        assertThat(response.failures()).isEmpty();
    }

    @Test
    @DisplayName("숙소 이름이 바뀐 채 다시 동기화하면 검색 결과에 바뀐 이름이 한 건으로 나온다")
    void returnRenamedStayOnceAfterResync() throws Exception {
        // given
        supplierServer.given(B_STAYS, bStays());
        supplierServer.given(A_STAYS, aStays(aHotel()));
        supplierServer.given(A_ROOMS, aRooms(aRoom()));
        sync();
        supplierServer.given(A_STAYS, aStays(aHotel(A_STAY_CODE, "에이 호텔 리뉴얼", aRoomType())));
        sync();

        // when
        StaySearchResponse response = searchResult(2);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::stayName)
                .containsExactly("에이 호텔 리뉴얼");
    }

    @Test
    @DisplayName("공급사 목록에서 빠진 숙소는 다시 동기화한 뒤 검색 결과에 나오지 않는다")
    void excludeStayDroppedFromSupplierList() throws Exception {
        // given
        supplierServer.given(B_STAYS, bStays());
        supplierServer.given(A_STAYS, aStays(
                aHotel(),
                aHotel("A-1002", "에이 호텔 별관", aRoomType("A-R09", "온돌 패밀리", 4))));
        supplierServer.given(A_ROOMS, aRooms(aRoom(), aRoom("A-1002", "A-R09")));
        sync();
        supplierServer.given(A_STAYS, aStays(aHotel()));
        sync();

        // when
        StaySearchResponse response = searchResult(2);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::stayName)
                .containsExactly(A_STAY_NAME);
    }

    @Test
    @DisplayName("공급사 목록에서 빠진 객실은 다시 동기화한 뒤 검색 결과에 나오지 않는다")
    void excludeRoomTypeDroppedFromSupplierList() throws Exception {
        // given
        supplierServer.given(B_STAYS, bStays());
        supplierServer.given(A_STAYS, aStays(
                aHotel(A_STAY_CODE, A_STAY_NAME, aRoomType(), aRoomType("A-R02", "이그제큐티브 스위트", 4))));
        supplierServer.given(A_ROOMS, aRooms(aRoom(), aRoom(A_STAY_CODE, "A-R02")));
        sync();
        supplierServer.given(A_STAYS, aStays(aHotel()));
        sync();

        // when
        StaySearchResponse response = searchResult(2);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::roomTypeName)
                .containsExactly(A_ROOM_TYPE_NAME);
    }

    @Test
    @DisplayName("공급사가 빈 목록을 주면 다시 동기화해도 그 공급사의 숙소가 그대로 나온다")
    void keepStaysWhenSupplierReturnsEmptyList() throws Exception {
        // given
        supplierServer.given(B_STAYS, bStays());
        supplierServer.given(A_STAYS, aStays(aHotel()));
        supplierServer.given(A_ROOMS, aRooms(aRoom()));
        sync();
        supplierServer.given(A_STAYS, aStays());
        sync();

        // when
        StaySearchResponse response = searchResult(2);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::stayName)
                .containsExactly(A_STAY_NAME);
    }

    @Test
    @DisplayName("한 공급사의 숙소가 전부 바뀌어도 다른 공급사의 숙소는 그대로 나온다")
    void keepOtherSupplierStaysWhenOneSupplierListChanges() throws Exception {
        // given
        supplierServer.given(A_STAYS, aStays(aHotel()));
        supplierServer.given(B_STAYS, bStays(bProperty()));
        supplierServer.given(A_ROOMS, aRooms(aRoom(), aRoom("A-1003", A_ROOM_TYPE_CODE)));
        supplierServer.given(B_ROOMS, bRooms(bRoom()));
        sync();
        supplierServer.given(A_STAYS, aStays(aHotel("A-1003", "에이 호텔 신관", aRoomType())));
        sync();

        // when
        StaySearchResponse response = searchResult(2);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::supplier, SearchedRoom::stayName)
                .containsExactlyInAnyOrder(
                        tuple(Supplier.A, "에이 호텔 신관"),
                        tuple(Supplier.B, B_STAY_NAME));
    }

    @Test
    @DisplayName("한 공급사의 목록 조회가 실패해도 다른 공급사의 숙소는 검색 결과에 나온다")
    void keepOtherSupplierStaysWhenOneSupplierListFails() throws Exception {
        // given
        supplierServer.given(A_STAYS, 503, aError("HOTEL_SERVICE_DOWN"));
        supplierServer.given(B_STAYS, bStays(bProperty()));
        supplierServer.given(B_ROOMS, bRooms(bRoom()));
        sync();

        // when
        StaySearchResponse response = searchResult(2);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::supplier, SearchedRoom::stayName)
                .containsExactly(tuple(Supplier.B, B_STAY_NAME));
    }
}
