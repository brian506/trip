package com.trip.stay.implement;

import com.trip.stay.implement.dto.b.BPropertiesData;
import com.trip.stay.implement.dto.b.BProperty;
import com.trip.stay.implement.dto.b.BResponse;
import com.trip.stay.implement.dto.b.BRoom;
import com.trip.stay.vo.Supplier;
import com.trip.stay.vo.SupplierFailureType;
import com.trip.stay.vo.SupplierRoomType;
import com.trip.stay.vo.SupplierStay;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.type.TypeReference;

// Supplier B: 항상 HTTP 200이고 본문 resultCode로 실패 표현.
@Component
public class SupplierBClient implements SupplierClient {

    private static final Supplier SUPPLIER = Supplier.B;
    private static final TypeReference<BResponse<BPropertiesData>> PROPERTIES_TYPE = new TypeReference<>() {
    };

    private final WebClient webClient;
    private final SupplierHttpCaller caller;

    public SupplierBClient(@Qualifier("supplierB") WebClient webClient, SupplierHttpCaller caller) {
        this.webClient = webClient;
        this.caller = caller;
    }

    @Override
    public Supplier supplier() {
        return SUPPLIER;
    }

    @Override
    public List<SupplierStay> fetchStays() {
        BPropertiesData data = caller.call(SUPPLIER, webClient.get().uri("/b/api/properties"),
                (status, body) -> read(status, body, PROPERTIES_TYPE));
        if (data.items() == null) {
            throw new SupplierCallException(SUPPLIER, SupplierFailureType.MALFORMED, "EMPTY_BODY");
        }
        return data.items().stream().map(this::toSupplierStay).toList();
    }

    // HTTP 상태 → 본문 파싱 → 본문 resultCode 순으로 실패를 본다. 성공인데 data가 없으면 MALFORMED.
    private <T> T read(HttpStatusCode status, String body, TypeReference<BResponse<T>> type) {
        if (status.isError()) {
            throw new SupplierCallException(SUPPLIER, SupplierErrors.classify(status), String.valueOf(status.value()));
        }
        BResponse<T> response = caller.parse(SUPPLIER, body, type);
        if (!response.isSuccess()) {
            throw new SupplierCallException(SUPPLIER, classify(response.resultCode()), response.resultCode());
        }
        if (response.data() == null) {
            throw new SupplierCallException(SUPPLIER, SupplierFailureType.MALFORMED, "NULL_DATA");
        }
        return response.data();
    }

    private SupplierFailureType classify(String resultCode) {
        if (resultCode == null) {
            return SupplierFailureType.MALFORMED;
        }
        return switch (resultCode) {
            case "E400" -> SupplierFailureType.BAD_REQUEST;
            case "E401" -> SupplierFailureType.AUTH;
            case "E429" -> SupplierFailureType.RATE_LIMITED;
            case "E503" -> SupplierFailureType.UNAVAILABLE;
            default -> SupplierFailureType.INTERNAL;
        };
    }

    private SupplierStay toSupplierStay(BProperty property) {
        List<SupplierRoomType> roomTypes = property.rooms() == null ? List.of() : property.rooms().stream()
                .map(this::toSupplierRoomType)
                .toList();
        return new SupplierStay(property.propertyId(), property.propertyName(), roomTypes);
    }

    private SupplierRoomType toSupplierRoomType(BRoom room) {
        return new SupplierRoomType(room.roomId(), room.roomName(), room.maxOccupancy() == null ? 0 : room.maxOccupancy());
    }
}
