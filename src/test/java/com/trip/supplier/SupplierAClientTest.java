package com.trip.supplier;

import com.trip.supplier.a.SupplierAClient;
import com.trip.supplier.fixture.SupplierPropertiesFixture;
import com.trip.supplier.global.SupplierHttpCaller;
import com.trip.supplier.vo.SupplierRoomType;
import com.trip.supplier.vo.SupplierStay;
import com.trip.supplier.vo.SupplierStayCodes;
import com.trip.support.exception.supplier.SupplierCallException;
import com.trip.support.exception.supplier.SupplierFailureType;
import com.trip.support.fixture.StayPeriodFixture;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.trip.supplier.fixture.SupplierResponseFixture.A_ROOM_TYPE_CODE;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_ROOM_TYPE_NAME;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_MAX_OCCUPANCY;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_STAY_CODE;
import static com.trip.supplier.fixture.SupplierResponseFixture.A_STAY_NAME;
import static com.trip.supplier.fixture.SupplierResponseFixture.aError;
import static com.trip.supplier.fixture.SupplierResponseFixture.aHotel;
import static com.trip.supplier.fixture.SupplierResponseFixture.aRoom;
import static com.trip.supplier.fixture.SupplierResponseFixture.aRooms;
import static com.trip.supplier.fixture.SupplierResponseFixture.aStays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

class SupplierAClientTest {

    private MockWebServer server;
    private SupplierAClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        SupplierHttpCaller caller = new SupplierHttpCaller(
                SupplierPropertiesFixture.properties(Duration.ofSeconds(5)),
                JsonMapper.builder().build()
        );
        client = new SupplierAClient(webClient, caller);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    private void enqueue(int status, String body) {
        server.enqueue(new MockResponse.Builder()
                .code(status)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    @Test
    @DisplayName("A의 정상 응답을 숙소 코드·이름·객실 목록으로 돌려준다")
    void readSuccessfulHotelsResponse() {
        // given
        enqueue(200, aStays(aHotel()));

        // when
        List<SupplierStay> stays = client.fetchStays();

        // then
        assertThat(stays)
                .extracting(SupplierStay::stayCode, SupplierStay::name)
                .containsExactly(tuple(A_STAY_CODE, A_STAY_NAME));
        assertThat(stays.getFirst().roomTypes())
                .extracting(SupplierRoomType::roomTypeCode, SupplierRoomType::name,
                        SupplierRoomType::maxOccupancy)
                .containsExactly(tuple(A_ROOM_TYPE_CODE, A_ROOM_TYPE_NAME, A_MAX_OCCUPANCY));
    }

    @Test
    @DisplayName("A에 보내는 재고·요금 요청은 숙소 코드를 쉼표로 이어 붙이고 기간·인원을 쿼리에 담는다")
    void sendStayCodesAndPeriodAsQuery() throws InterruptedException {
        // given
        enqueue(200, aRooms(aRoom()));
        StayPeriod period = StayPeriodFixture.twoNights();
        Set<String> codes = new LinkedHashSet<>(List.of(A_STAY_CODE, "A-1002"));

        // when
        client.fetchRooms(period, new Guests(2, 1), new SupplierStayCodes(codes));

        // then
        RecordedRequest request = server.takeRequest();
        HttpUrl url = request.getUrl();
        assertThat(url.encodedPath()).isEqualTo("/a/v1/availability");
        assertThat(url.queryParameter("hotelCodes")).isEqualTo("A-1001,A-1002");
        assertThat(url.queryParameter("checkIn")).isEqualTo(period.checkIn().toString());
        assertThat(url.queryParameter("checkOut")).isEqualTo(period.checkOut().toString());
        assertThat(url.queryParameter("adults")).isEqualTo("2");
        assertThat(url.queryParameter("children")).isEqualTo("1");
    }

    @Test
    @DisplayName("A의 실패 응답은 본문의 error 코드를 실패 코드로 남긴다")
    void keepErrorCodeFromFailureBody() {
        // given
        enqueue(503, aError("HOTEL_SERVICE_DOWN"));

        // when
        Throwable thrown = catchThrowable(() -> client.fetchStays());

        // then
        assertThat(thrown).isInstanceOf(SupplierCallException.class);
        assertThat(((SupplierCallException) thrown).toFailure())
                .extracting(f -> f.supplier(), f -> f.type(), f -> f.code())
                .containsExactly(Supplier.A, SupplierFailureType.UNAVAILABLE, "HOTEL_SERVICE_DOWN");
    }

    @Test
    @DisplayName("A의 실패 본문에 error 코드가 없으면 상태 코드 숫자를 실패 코드로 남긴다")
    void fallBackToStatusCodeWhenFailureBodyHasNoErrorCode() {
        // given
        enqueue(500, "{}");

        // when
        Throwable thrown = catchThrowable(() -> client.fetchStays());

        // then
        assertThat(thrown).isInstanceOf(SupplierCallException.class);
        assertThat(((SupplierCallException) thrown).toFailure())
                .extracting(f -> f.type(), f -> f.code())
                .containsExactly(SupplierFailureType.INTERNAL, "500");
    }
}
