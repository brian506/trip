package com.trip.stay.controller;

import com.trip.stay.controller.response.StaySearchResponse;
import com.trip.stay.vo.SearchedRoom;
import com.trip.supplier.Supplier;
import com.trip.supplier.vo.SupplierFailure;
import com.trip.support.exception.supplier.SupplierFailureType;
import com.trip.support.fixture.StayPeriodFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.trip.supplier.fixture.SupplierResponseFixture.A_STAY_NAME;
import static com.trip.supplier.fixture.SupplierResponseFixture.B_STAY_NAME;
import static com.trip.supplier.fixture.SupplierResponseFixture.aError;
import static com.trip.supplier.fixture.SupplierResponseFixture.aHotel;
import static com.trip.supplier.fixture.SupplierResponseFixture.aRoom;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaySearchApiTest extends StayApiTest {

    private void syncBothSuppliers() throws Exception {
        supplierServer.given(A_STAYS, aStays(aHotel()));
        supplierServer.given(B_STAYS, bStays(bProperty()));
        sync();
    }

    @Test
    @DisplayName("인원 수를 수용하지 못하는 객실은 검색 결과에 나오지 않는다")
    void excludeRoomThatCannotHoldGuests() throws Exception {
        // given
        syncBothSuppliers();
        supplierServer.given(A_ROOMS, aRooms(aRoom()));
        supplierServer.given(B_ROOMS, bRooms(bRoom()));

        // when
        StaySearchResponse response = searchResult(3);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::supplier, SearchedRoom::stayName)
                .containsExactly(tuple(Supplier.A, A_STAY_NAME));
    }

    @Test
    @DisplayName("한 공급사만 실패하면 200으로 나머지 결과와 실패 목록을 함께 준다")
    void returnRemainingRoomsWithFailureWhenOneSupplierFails() throws Exception {
        // given
        syncBothSuppliers();
        supplierServer.given(A_ROOMS, 503, aError("HOTEL_SERVICE_DOWN"));
        supplierServer.given(B_ROOMS, bRooms(bRoom()));

        // when
        StaySearchResponse response = searchResult(2);

        // then
        assertThat(response.rooms())
                .extracting(SearchedRoom::supplier, SearchedRoom::stayName)
                .containsExactly(tuple(Supplier.B, B_STAY_NAME));
        assertThat(response.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type, SupplierFailure::code)
                .containsExactly(tuple(Supplier.A, SupplierFailureType.UNAVAILABLE, "HOTEL_SERVICE_DOWN"));
    }

    @Test
    @DisplayName("모든 공급사가 실패하면 502 E2006과 공급사별 실패 내역을 준다")
    void returnBadGatewayWhenEverySupplierFails() throws Exception {
        // given
        syncBothSuppliers();
        supplierServer.given(A_ROOMS, 500, aError("HOTEL_INTERNAL"));
        supplierServer.given(B_ROOMS, 500, "{}");

        // when
        var result = search(2);

        // then
        result.andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.resultType").value("ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.errorCode").value("E2006"))
                .andExpect(jsonPath("$.error.data.length()").value(2))
                .andExpect(jsonPath("$.error.data[*].supplier", containsInAnyOrder("A", "B")))
                .andExpect(jsonPath("$.error.data[*].type", containsInAnyOrder("INTERNAL", "INTERNAL")))
                .andExpect(jsonPath("$.error.data[*].code", containsInAnyOrder("HOTEL_INTERNAL", "500")));
    }

    @Test
    @DisplayName("체크인 날짜가 없으면 400 E400을 준다")
    void returnBadRequestWhenCheckInMissing() throws Exception {
        // given
        syncBothSuppliers();

        // when
        var result = mockMvc.perform(get("/api/v1/stays/search")
                .param("checkOut", PERIOD.checkOut().toString())
                .param("adults", "2"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorCode").value("E400"))
                .andExpect(jsonPath("$.error.data.checkIn").value("체크인 날짜는 필수입니다"));
    }

    @Test
    @DisplayName("체크인이 지난 날짜면 400 E1002를 준다")
    void returnBadRequestWhenCheckInIsPast() throws Exception {
        // given
        syncBothSuppliers();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // when
        var result = mockMvc.perform(get("/api/v1/stays/search")
                .param("checkIn", yesterday.toString())
                .param("checkOut", StayPeriodFixture.day(1).toString())
                .param("adults", "2"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorCode").value("E1002"));
    }

    @Test
    @DisplayName("체크인이 오늘이면 검색을 받아들인다")
    void acceptSearchWhenCheckInIsToday() throws Exception {
        // given
        syncBothSuppliers();
        supplierServer.given(A_ROOMS, aRooms(aRoom()));
        supplierServer.given(B_ROOMS, bRooms(bRoom()));
        LocalDate today = LocalDate.now();

        // when
        var result = mockMvc.perform(get("/api/v1/stays/search")
                .param("checkIn", today.toString())
                .param("checkOut", today.plusDays(1).toString())
                .param("adults", "2"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
