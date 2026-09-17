package com.trip.supplier.a;

import com.trip.supplier.Supplier;
import com.trip.supplier.SupplierClient;
import com.trip.supplier.a.response.AAvailabilityResponse;
import com.trip.supplier.a.response.AErrorResponse;
import com.trip.supplier.a.response.AHotel;
import com.trip.supplier.a.response.AHotelsResponse;
import com.trip.support.exception.supplier.SupplierCallException;
import com.trip.support.exception.supplier.SupplierErrors;
import com.trip.support.exception.supplier.SupplierFailureType;
import com.trip.supplier.global.SupplierHttpCaller;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.supplier.vo.SupplierStay;
import com.trip.supplier.vo.SupplierStayCodes;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// Supplier A: HTTP 상태 코드로 실패를 표현하고, 실패 본문에 error 코드를 준다.
@Component
public class SupplierAClient implements SupplierClient {

    private static final Supplier SUPPLIER = Supplier.A;

    private final WebClient webClient;
    private final SupplierHttpCaller caller;

    public SupplierAClient(@Qualifier("supplierA") WebClient webClient, SupplierHttpCaller caller) {
        this.webClient = webClient;
        this.caller = caller;
    }

    @Override
    public Supplier supplier() {
        return SUPPLIER;
    }

    @Override
    public List<SupplierStay> fetchStays() {
        AHotelsResponse response = caller.call(SUPPLIER, webClient.get().uri("/a/v1/hotels"),
                (status, body) -> read(status, body, AHotelsResponse.class));
        if (response.items() == null) {
            throw new SupplierCallException(SUPPLIER, SupplierFailureType.MALFORMED, "EMPTY_BODY");
        }
        return response.items().stream().map(AHotel::toSupplierStay).toList();
    }

    @Override
    public List<SupplierRoom> fetchRooms(StayPeriod period, Guests guests, SupplierStayCodes stayCodes) {
        AAvailabilityResponse response = caller.call(SUPPLIER, webClient.get().uri(builder -> builder
                        .path("/a/v1/availability")
                        .queryParam("hotelCodes", stayCodes.joined())
                        .queryParam("checkIn", period.checkIn())
                        .queryParam("checkOut", period.checkOut())
                        .queryParam("adults", guests.adults())
                        .queryParam("children", guests.children())
                        .build()),
                (status, body) -> read(status, body, AAvailabilityResponse.class));
        if (response.items() == null) {
            throw new SupplierCallException(SUPPLIER, SupplierFailureType.MALFORMED, "EMPTY_BODY");
        }
        return response.toSupplierRooms(period);
    }

    // HTTP 상태로 실패를 분류하고 본문 error 코드를 원본 코드로 남긴다. 성공이면 본문을 type으로 읽는다.
    private <T> T read(HttpStatusCode status, String body, Class<T> type) {
        if (status.isError()) {
            throw new SupplierCallException(SUPPLIER, SupplierErrors.classify(status), errorCode(status, body));
        }
        return caller.parse(SUPPLIER, body, type);
    }

    // 실패 본문이 없거나 깨졌으면 상태 코드 숫자를 원본 코드로 쓴다.
    private String errorCode(HttpStatusCode status, String body) {
        String statusCode = String.valueOf(status.value());
        try {
            AErrorResponse error = caller.parse(SUPPLIER, body, AErrorResponse.class);
            return error.error() == null ? statusCode : error.error();
        } catch (SupplierCallException e) {
            return statusCode;
        }
    }
}
