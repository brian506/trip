package com.trip.supplier.global;

import com.trip.supplier.Supplier;
import com.trip.supplier.SupplierClient;
import com.trip.supplier.fixture.SupplierPropertiesFixture;
import com.trip.supplier.fixture.SupplierRoomFixture;
import com.trip.supplier.fixture.SupplierStayCodesFixture;
import com.trip.supplier.vo.SupplierDispatchResult;
import com.trip.supplier.vo.SupplierFailure;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.support.exception.supplier.SupplierCallException;
import com.trip.support.exception.supplier.SupplierFailureType;
import com.trip.support.fixture.StayPeriodFixture;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SupplierDispatcherTest {

    private static final StayPeriod PERIOD = StayPeriodFixture.twoNights();
    private static final Guests GUESTS = new Guests(2, 1);

    @Mock
    private SupplierClient clientA;

    @Mock
    private SupplierClient clientB;

    private ExecutorService supplierExecutor;

    @BeforeEach
    void setUp() {
        supplierExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @AfterEach
    void tearDown() {
        supplierExecutor.close();
    }

    private SupplierDispatcher dispatcher(Duration totalTimeout) {
        SupplierProperties properties = SupplierPropertiesFixture.properties(totalTimeout);
        return new SupplierDispatcher(
                List.of(clientA, clientB),
                properties,
                supplierExecutor,
                new SupplierCircuitBreaker(properties)
        );
    }

    private static Map<Supplier, Set<String>> stayCodes(int countForA, int countForB) {
        return Map.of(
                Supplier.A, SupplierStayCodesFixture.stayCodes(countForA),
                Supplier.B, SupplierStayCodesFixture.stayCodes(countForB)
        );
    }

    private static SupplierCallException failure(Supplier supplier, SupplierFailureType type) {
        return new SupplierCallException(supplier, type, type.name());
    }

    @Test
    @DisplayName("공급사 두 곳이 모두 응답하면 두 곳의 상품을 합쳐 돌려준다")
    void mergeRoomsFromBothSuppliers() {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any()))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1001", "A-R01")));
        given(clientB.fetchRooms(any(), any(), any()))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.B, "B-2001", "B-R01")));

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofSeconds(5))
                .dispatch(PERIOD, GUESTS, stayCodes(1, 1));

        // then
        assertThat(result.failures()).isEmpty();
        assertThat(result.rooms())
                .extracting(SupplierRoom::supplier, SupplierRoom::stayCode, SupplierRoom::roomTypeCode)
                .containsExactlyInAnyOrder(
                        tuple(Supplier.A, "A-1001", "A-R01"),
                        tuple(Supplier.B, "B-2001", "B-R01")
                );
    }

    @Test
    @DisplayName("숙소 코드가 50개를 넘으면 묶음으로 나눠 부르고 결과를 모두 합친다")
    void mergeRoomsAcrossBatches() {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any()))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1001", "A-R01")))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1002", "A-R02")));
        given(clientB.fetchRooms(any(), any(), any())).willReturn(List.of());

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofSeconds(5))
                .dispatch(PERIOD, GUESTS, stayCodes(51, 1));

        // then
        assertThat(result.failures()).isEmpty();
        assertThat(result.rooms())
                .extracting(SupplierRoom::stayCode)
                .containsExactlyInAnyOrder("A-1001", "A-1002");
    }

    @Test
    @DisplayName("앞 묶음이 성공한 뒤 뒤 묶음이 실패해도 앞 묶음 상품은 살린다")
    void keepRoomsFromEarlierBatchesWhenLaterBatchFails() {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any()))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1001", "A-R01")))
                .willThrow(failure(Supplier.A, SupplierFailureType.UNAVAILABLE));
        given(clientB.fetchRooms(any(), any(), any())).willReturn(List.of());

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofSeconds(5))
                .dispatch(PERIOD, GUESTS, stayCodes(120, 1));

        // then
        assertThat(result.rooms())
                .extracting(SupplierRoom::stayCode)
                .containsExactly("A-1001");
        assertThat(result.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type)
                .containsExactly(tuple(Supplier.A, SupplierFailureType.UNAVAILABLE));
    }

    @Test
    @DisplayName("한 곳이 실패하면 나머지 공급사 상품과 그 공급사의 실패를 함께 돌려준다")
    void returnRoomsFromHealthySupplierWithFailureOfTheOther() {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any()))
                .willThrow(failure(Supplier.A, SupplierFailureType.UNAVAILABLE));
        given(clientB.fetchRooms(any(), any(), any()))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.B, "B-2001", "B-R01")));

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofSeconds(5))
                .dispatch(PERIOD, GUESTS, stayCodes(1, 1));

        // then
        assertThat(result.rooms())
                .extracting(SupplierRoom::supplier, SupplierRoom::stayCode)
                .containsExactly(tuple(Supplier.B, "B-2001"));
        assertThat(result.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type)
                .containsExactly(tuple(Supplier.A, SupplierFailureType.UNAVAILABLE));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SupplierFailureType.class, names = {"AUTH", "RATE_LIMITED", "UNAVAILABLE"})
    @DisplayName("AUTH·RATE_LIMITED·UNAVAILABLE로 실패하면 남은 묶음 상품이 결과에 없다")
    void stopRemainingBatchesWhenSupplierIsDownOrRefusing(SupplierFailureType type) {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any()))
                .willThrow(failure(Supplier.A, type))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1002", "A-R02")))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1003", "A-R03")));
        given(clientB.fetchRooms(any(), any(), any())).willReturn(List.of());

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofSeconds(5))
                .dispatch(PERIOD, GUESTS, stayCodes(120, 1));

        // then
        assertThat(result.rooms()).isEmpty();
        assertThat(result.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type)
                .containsExactly(tuple(Supplier.A, type));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SupplierFailureType.class, names = {"BAD_REQUEST", "MALFORMED", "INTERNAL"})
    @DisplayName("BAD_REQUEST·MALFORMED·INTERNAL로 실패해도 남은 묶음 상품은 결과에 있다")
    void continueRemainingBatchesWhenFailureIsBatchLocal(SupplierFailureType type) {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any()))
                .willThrow(failure(Supplier.A, type))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1002", "A-R02")))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1003", "A-R03")));
        given(clientB.fetchRooms(any(), any(), any())).willReturn(List.of());

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofSeconds(5))
                .dispatch(PERIOD, GUESTS, stayCodes(120, 1));

        // then
        assertThat(result.rooms())
                .extracting(SupplierRoom::stayCode)
                .containsExactlyInAnyOrder("A-1002", "A-1003");
        assertThat(result.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type)
                .containsExactly(tuple(Supplier.A, type));
    }

    @Test
    @DisplayName("한 공급사의 묶음 두 개가 모두 실패해도 실패는 한 건만 남는다")
    void keepOnlyOneFailurePerSupplier() {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any()))
                .willThrow(failure(Supplier.A, SupplierFailureType.BAD_REQUEST))
                .willThrow(failure(Supplier.A, SupplierFailureType.INTERNAL))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1003", "A-R03")));
        given(clientB.fetchRooms(any(), any(), any())).willReturn(List.of());

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofSeconds(5))
                .dispatch(PERIOD, GUESTS, stayCodes(120, 1));

        // then
        assertThat(result.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type)
                .containsExactly(tuple(Supplier.A, SupplierFailureType.BAD_REQUEST));
        assertThat(result.rooms())
                .extracting(SupplierRoom::stayCode)
                .containsExactly("A-1003");
    }

    @Test
    @DisplayName("전체 타임아웃을 넘긴 공급사는 UNAVAILABLE/TOTAL_TIMEOUT_EXCEEDED로 남는다")
    void markSupplierExceedingTotalTimeout() {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any())).willAnswer(invocation -> {
            Thread.sleep(2_000);
            return List.of();
        });
        given(clientB.fetchRooms(any(), any(), any()))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.B, "B-2001", "B-R01")));

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofMillis(300))
                .dispatch(PERIOD, GUESTS, stayCodes(1, 1));

        // then
        assertThat(result.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type, SupplierFailure::code)
                .containsExactly(tuple(Supplier.A, SupplierFailureType.UNAVAILABLE, "TOTAL_TIMEOUT_EXCEEDED"));
        assertThat(result.rooms())
                .extracting(SupplierRoom::stayCode)
                .containsExactly("B-2001");
    }

    @Test
    @DisplayName("태스크에서 예기치 못한 예외가 나면 MALFORMED/UNEXPECTED로 남고 상품도 실패도 사라지지 않는다")
    void markUnexpectedExceptionAsMalformed() {
        // given
        given(clientA.supplier()).willReturn(Supplier.A);
        given(clientB.supplier()).willReturn(Supplier.B);
        given(clientA.fetchRooms(any(), any(), any()))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.A, "A-1001", "A-R01")))
                .willThrow(new IllegalStateException("예기치 못한 오류"));
        given(clientB.fetchRooms(any(), any(), any()))
                .willReturn(List.of(SupplierRoomFixture.room(Supplier.B, "B-2001", "B-R01")));

        // when
        SupplierDispatchResult result = dispatcher(Duration.ofSeconds(5))
                .dispatch(PERIOD, GUESTS, stayCodes(120, 1));

        // then
        assertThat(result.failures())
                .extracting(SupplierFailure::supplier, SupplierFailure::type, SupplierFailure::code)
                .containsExactly(tuple(Supplier.A, SupplierFailureType.MALFORMED, "UNEXPECTED"));
        assertThat(result.rooms())
                .extracting(SupplierRoom::stayCode)
                .containsExactlyInAnyOrder("A-1001", "B-2001");
    }
}
