package com.trip.external.supplier;

import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class SupplierHttpCaller {

    private final SupplierProperties properties;
    private final JsonMapper jsonMapper;

    public <T> T call(Supplier supplier, WebClient.RequestHeadersSpec<?> request,
                      BiFunction<HttpStatusCode, String, T> reader) {
        ResponseEntity<String> response;
        try {
            response = request.exchangeToMono(res -> res.toEntity(String.class))
                    .timeout(properties.responseTimeout())
                    .block();
        } catch (RuntimeException e) {
            throw SupplierErrors.translate(supplier, e);
        }
        return reader.apply(response.getStatusCode(), response.getBody());
    }

    public <T> T parse(Supplier supplier, String body, Class<T> type) {
        return parse(supplier, body, jsonMapper.getTypeFactory().constructType(type));
    }

    public <T> T parse(Supplier supplier, String body, TypeReference<T> type) {
        return parse(supplier, body, jsonMapper.getTypeFactory().constructType(type));
    }

    private <T> T parse(Supplier supplier, String body, JavaType type) {
        if (body == null || body.isBlank()) {
            throw new SupplierCallException(supplier, SupplierFailureType.MALFORMED, "EMPTY_BODY");
        }
        T value;
        try {
            value = jsonMapper.readValue(body, type);
        } catch (JacksonException e) {
            throw new SupplierCallException(supplier, SupplierFailureType.MALFORMED, "UNPARSEABLE", e);
        }
        if (value == null) {
            throw new SupplierCallException(supplier, SupplierFailureType.MALFORMED, "EMPTY_BODY");
        }
        return value;
    }
}
