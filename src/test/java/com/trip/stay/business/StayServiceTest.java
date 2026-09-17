package com.trip.stay.business;

import com.trip.stay.controller.response.StaySearchResponse;
import com.trip.stay.dataaccess.entity.RoomType;
import com.trip.stay.dataaccess.entity.Stay;
import com.trip.stay.dataaccess.repository.RoomTypeRepository;
import com.trip.stay.dataaccess.repository.StayRepository;
import com.trip.stay.fixture.StayEntityFixture;
import com.trip.stay.implement.RoomTypeManager;
import com.trip.stay.implement.StayManager;
import com.trip.stay.vo.SearchedRoom;
import com.trip.supplier.Supplier;
import com.trip.supplier.fixture.SupplierPropertiesFixture;
import com.trip.supplier.global.SupplierDispatcher;
import com.trip.supplier.global.SupplierRetry;
import com.trip.supplier.vo.SupplierDispatchResult;
import com.trip.supplier.vo.SupplierFailure;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import com.trip.support.exception.supplier.SupplierFailureType;
import com.trip.support.fixture.StayPeriodFixture;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StayServiceTest {

    private static final StayPeriod PERIOD = StayPeriodFixture.twoNights();
    private static final Guests GUESTS = new Guests(2, 1);

    @Mock
    private StayRepository stayRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private SupplierDispatcher supplierDispatcher;

    private StayService stayService;

    private Stay stayA;
    private Stay stayB;
    private RoomType roomTypeA;
    private RoomType roomTypeB;

    @BeforeEach
    void setUp() {
        StayManager stayManager = new StayManager(stayRepository, new RoomTypeManager(roomTypeRepository));
        SupplierRetry supplierRetry =
                new SupplierRetry(SupplierPropertiesFixture.properties(Duration.ofSeconds(5)));
        stayService = new StayService(List.of(), stayManager, supplierDispatcher, supplierRetry);

        stayA = StayEntityFixture.stay(Supplier.A, "A-1001");
        stayB = StayEntityFixture.stay(Supplier.B, "B-2001");
        roomTypeA = StayEntityFixture.roomType(stayA, "A-R01", 3);
        roomTypeB = StayEntityFixture.roomType(stayB, "B-R01", 2);
    }

    private void givenCandidates() {
        given(stayRepository.findAllByActiveTrue()).willReturn(List.of(stayA, stayB));
        given(roomTypeRepository.findAccommodatable(any(), anyInt()))
                .willReturn(List.of(roomTypeA, roomTypeB));
    }

    private static SupplierRoom roomOf(Stay stay, RoomType roomType, int availableRooms, long totalPrice) {
        return new SupplierRoom(
                stay.getSupplier(),
                stay.getStayCode(),
                roomType.getRoomTypeCode(),
                availableRooms,
                true,
                "KRW",
                totalPrice
        );
    }

    @Test
    @DisplayName("공급사 상품을 후보 정보와 합쳐 rooms에 담는다")
    void mergeSupplierRoomsWithCandidates() {
        // given
        givenCandidates();
        given(supplierDispatcher.dispatch(any(), any(), any())).willReturn(new SupplierDispatchResult(
                List.of(
                        roomOf(stayA, roomTypeA, 4, 275_000L),
                        roomOf(stayB, roomTypeB, 6, 452_000L)
                ),
                List.of(),
                2
        ));

        // when
        StaySearchResponse response = stayService.search(PERIOD, GUESTS);

        // then
        assertThat(response.failures()).isEmpty();
        assertThat(response.rooms())
                .extracting(SearchedRoom::stayId, SearchedRoom::stayName, SearchedRoom::roomTypeId,
                        SearchedRoom::availableRooms, SearchedRoom::totalPrice)
                .containsExactlyInAnyOrder(
                        tuple(stayA.getId(), stayA.getName(), roomTypeA.getId(), 4, 275_000L),
                        tuple(stayB.getId(), stayB.getName(), roomTypeB.getId(), 6, 452_000L)
                );
    }

    @Test
    @DisplayName("부분 실패를 failures에 공급사별 한 건으로 담는다")
    void exposePartialFailurePerSupplier() {
        // given
        givenCandidates();
        given(supplierDispatcher.dispatch(any(), any(), any())).willReturn(new SupplierDispatchResult(
                List.of(roomOf(stayB, roomTypeB, 6, 452_000L)),
                List.of(new SupplierFailure(Supplier.A, SupplierFailureType.UNAVAILABLE, "E503")),
                2
        ));

        // when
        StaySearchResponse response = stayService.search(PERIOD, GUESTS);

        // then
        assertThat(response.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type, SupplierFailure::code)
                .containsExactly(tuple(Supplier.A, SupplierFailureType.UNAVAILABLE, "E503"));
        assertThat(response.rooms())
                .extracting(SearchedRoom::roomTypeId)
                .containsExactly(roomTypeB.getId());
    }

    @Test
    @DisplayName("한 곳이 0건을 정상으로 주고 다른 곳이 실패하면 예외를 던지지 않는다")
    void doNotFailWhenOneSupplierReturnsNoRooms() {
        // given
        givenCandidates();
        given(supplierDispatcher.dispatch(any(), any(), any())).willReturn(new SupplierDispatchResult(
                List.of(),
                List.of(new SupplierFailure(Supplier.A, SupplierFailureType.UNAVAILABLE, "E503")),
                2
        ));

        // when
        StaySearchResponse response = stayService.search(PERIOD, GUESTS);

        // then
        assertThat(response.rooms()).isEmpty();
        assertThat(response.failures())
                .extracting(SupplierFailure::supplier)
                .containsExactly(Supplier.A);
    }

    @Test
    @DisplayName("물어본 공급사가 모두 실패하면 SUPPLIER_ALL_FAILED를 던지고 실패 목록을 data에 담는다")
    void throwWhenEverySupplierFails() {
        // given
        givenCandidates();
        List<SupplierFailure> failures = List.of(
                new SupplierFailure(Supplier.A, SupplierFailureType.UNAVAILABLE, "E503"),
                new SupplierFailure(Supplier.B, SupplierFailureType.INTERNAL, "E500")
        );
        given(supplierDispatcher.dispatch(any(), any(), any()))
                .willReturn(new SupplierDispatchResult(List.of(), failures, 2));

        // when
        Throwable thrown = catchThrowable(() -> stayService.search(PERIOD, GUESTS));

        // then
        assertThat(thrown).isInstanceOf(AppException.class);
        AppException appException = (AppException) thrown;
        assertThat(appException.getErrorType()).isEqualTo(ErrorType.SUPPLIER_ALL_FAILED);
        assertThat(appException.getData())
                .asInstanceOf(list(SupplierFailure.class))
                .extracting(SupplierFailure::supplier, SupplierFailure::type)
                .containsExactlyInAnyOrder(
                        tuple(Supplier.A, SupplierFailureType.UNAVAILABLE),
                        tuple(Supplier.B, SupplierFailureType.INTERNAL)
                );
    }
}
