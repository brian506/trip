package com.trip.supplier;

import com.trip.supplier.b.SupplierBClient;
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

import static com.trip.supplier.fixture.SupplierResponseFixture.B_STAY_CODE;
import static com.trip.supplier.fixture.SupplierResponseFixture.bRoom;
import static com.trip.supplier.fixture.SupplierResponseFixture.bRooms;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

class SupplierBClientTest {

    private MockWebServer server;
    private SupplierBClient client;

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
        client = new SupplierBClient(webClient, caller);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    @DisplayName("B의 정상 응답을 숙소 코드·이름·객실 목록으로 돌려준다")
    void readSuccessfulPropertiesResponse() {
        // given
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {"resultCode":"0000","resultMessage":"OK","data":{"items":[
                          {"propertyId":"B-2001","propertyName":"비 프로퍼티","rooms":[
                            {"roomId":"B-R01","roomName":"스탠다드 더블","maxOccupancy":2}
                          ]}
                        ]}}
                        """)
                .build());

        // when
        List<SupplierStay> stays = client.fetchStays();

        // then
        assertThat(stays)
                .extracting(SupplierStay::stayCode, SupplierStay::name)
                .containsExactly(tuple("B-2001", "비 프로퍼티"));
        assertThat(stays.getFirst().roomTypes())
                .extracting(SupplierRoomType::roomTypeCode, SupplierRoomType::name,
                        SupplierRoomType::maxOccupancy)
                .containsExactly(tuple("B-R01", "스탠다드 더블", 2));
    }

    @Test
    @DisplayName("B는 HTTP 200이어도 본문 resultCode가 E503이면 UNAVAILABLE로 실패한다")
    void treatBodyResultCodeAsFailureDespiteHttpOk() {
        // given
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {"resultCode":"E503","resultMessage":"서비스를 사용할 수 없습니다","data":null}
                        """)
                .build());

        // when
        Throwable thrown = catchThrowable(() -> client.fetchStays());

        // then
        assertThat(thrown).isInstanceOf(SupplierCallException.class);
        assertThat(((SupplierCallException) thrown).toFailure())
                .extracting(failure -> failure.supplier(), failure -> failure.type(), failure -> failure.code())
                .containsExactly(Supplier.B, SupplierFailureType.UNAVAILABLE, "E503");
    }

    @Test
    @DisplayName("B에 보내는 재고·요금 요청은 숙소 코드를 쉼표로 이어 붙이고 기간·인원을 쿼리에 담는다")
    void sendStayCodesAndPeriodAsQuery() throws InterruptedException {
        // given
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(bRooms(bRoom()))
                .build());
        StayPeriod period = StayPeriodFixture.twoNights();
        Set<String> codes = new LinkedHashSet<>(List.of(B_STAY_CODE, "B-2002"));

        // when
        client.fetchRooms(period, new Guests(2, 1), new SupplierStayCodes(codes));

        // then
        RecordedRequest request = server.takeRequest();
        HttpUrl url = request.getUrl();
        assertThat(url.encodedPath()).isEqualTo("/b/api/search");
        assertThat(url.queryParameter("propertyIds")).isEqualTo("B-2001,B-2002");
        assertThat(url.queryParameter("checkIn")).isEqualTo(period.checkIn().toString());
        assertThat(url.queryParameter("checkOut")).isEqualTo(period.checkOut().toString());
        assertThat(url.queryParameter("adults")).isEqualTo("2");
        assertThat(url.queryParameter("children")).isEqualTo("1");
    }
}
