package com.trip.support;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// A와 B는 경로가 갈려서 한 서버로 둘 다 받는다. 경로마다 응답을 걸어 두면 묶음이 여러 번 와도 같은 응답을 준다.
public final class SupplierMockServer {

    public static final String A_STAYS = "/a/v1/hotels";
    public static final String A_ROOMS = "/a/v1/availability";
    public static final String B_STAYS = "/b/api/properties";
    public static final String B_ROOMS = "/b/api/search";

    private final MockWebServer server = new MockWebServer();
    private final Map<String, MockResponse> responses = new ConcurrentHashMap<>();
    private final Map<String, Headers> receivedHeaders = new ConcurrentHashMap<>();

    public SupplierMockServer() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getUrl().encodedPath();
                receivedHeaders.put(path, request.getHeaders());
                return responses.getOrDefault(path, response(404, "{}"));
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String baseUrl() {
        return "http://localhost:" + server.getPort();
    }

    public void given(String path, String body) {
        responses.put(path, response(200, body));
    }

    public void given(String path, int status, String body) {
        responses.put(path, response(status, body));
    }

    public String receivedHeader(String path, String name) {
        Headers headers = receivedHeaders.get(path);
        return headers == null ? null : headers.get(name);
    }

    public void reset() {
        responses.clear();
        receivedHeaders.clear();
    }

    private static MockResponse response(int status, String body) {
        return new MockResponse.Builder()
                .code(status)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }
}
