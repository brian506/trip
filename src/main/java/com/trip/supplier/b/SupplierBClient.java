package com.trip.supplier.b;

import com.trip.supplier.Supplier;
import com.trip.supplier.SupplierClient;
import com.trip.supplier.b.response.BPropertiesData;
import com.trip.supplier.b.response.BProperty;
import com.trip.supplier.b.response.BResponse;
import com.trip.supplier.exception.SupplierCallException;
import com.trip.supplier.exception.SupplierErrors;
import com.trip.supplier.exception.SupplierFailureType;
import com.trip.supplier.infra.SupplierHttpCaller;
import com.trip.supplier.vo.SupplierStay;
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
        return data.items().stream().map(BProperty::toSupplierStay).toList();
    }

    // HTTP 상태 → 본문 파싱 → 본문 resultCode 순으로 실패를 본다. 성공인데 data가 없으면 MALFORMED.
    private <T> T read(HttpStatusCode status, String body, TypeReference<BResponse<T>> type) {
        if (status.isError()) {
            throw new SupplierCallException(SUPPLIER, SupplierErrors.classify(status), String.valueOf(status.value()));
        }
        BResponse<T> response = caller.parse(SUPPLIER, body, type);
        if (!response.isSuccess()) {
            throw new SupplierCallException(SUPPLIER, BResultCode.classify(response.resultCode()), response.resultCode());
        }
        if (response.data() == null) {
            throw new SupplierCallException(SUPPLIER, SupplierFailureType.MALFORMED, "NULL_DATA");
        }
        return response.data();
    }
}
