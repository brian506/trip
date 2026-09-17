package com.trip.mock.supplier;

import java.time.LocalDate;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 공급사 A·B를 흉내 내는 Mock. 숙소 코드와 인원은 무시하고 고정 목록을 준다.
@RestController
public class MockSupplierController {

    private static final String NORMAL = "normal";
    private static final String ERROR = "error";
    private static final String NO_RESPONSE = "no-response";

    // 무응답은 연결만 되고 응답이 오지 않는 상황이다. 검색 전체 타임아웃(5s)보다 훨씬 길게 잡아 사실상 오지 않게 한다.
    private static final long NO_RESPONSE_MILLIS = 600_000L;

    // 숙박일마다 돌려 쓰는 {잔여 수, 1박 요금(net), 세액}. A-10044는 둘째 날 잔여가 0이라 availableRooms=0 경로를 만든다.
    private static final int[][] A_10023_RATES = {{3, 120_000, 12_000}, {1, 150_000, 15_000}, {5, 120_000, 12_000}};
    private static final int[][] A_10044_RATES = {{2, 88_000, 8_800}, {0, 99_000, 9_900}, {4, 88_000, 8_800}};
    private static final int[] B_77120_REMAINING = {3, 1, 5};

    // 공급사별 모드. normal | error | no-response
    private final Map<String, String> modes = new ConcurrentHashMap<>();

    @PostMapping("/control/{supplier}/mode")
    public Map<String, String> setMode(@PathVariable String supplier, @RequestParam String value) {
        modes.put(supplier, value);
        return Map.of(supplier, value);
    }

    // ── ① 숙소 목록 (정적 콘텐츠) ─────────────────────────────

    // 목록 API도 재고·요금과 같은 모드를 따른다. 동기화 재시도를 확인하려면 여기서도 장애가 나야 한다.

    @GetMapping(value = "/a/v1/hotels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> hotelsA() {
        return switch (modeOf("a")) {
            case ERROR -> ResponseEntity.status(503)
                    .body("""
                            {"error":"SERVICE_UNAVAILABLE","message":"temporarily unavailable"}""");
            case NO_RESPONSE -> noResponse();
            default -> ResponseEntity.ok(A_HOTELS);
        };
    }

    @GetMapping(value = "/b/api/properties", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> propertiesB() {
        return switch (modeOf("b")) {
            case ERROR -> ResponseEntity.ok("""
                    {"resultCode":"E503","resultMessage":"TEMPORARILY_UNAVAILABLE","data":null}""");
            case NO_RESPONSE -> noResponse();
            default -> ResponseEntity.ok(B_PROPERTIES);
        };
    }

    // ── ② 재고·요금 조회 (숙소 코드 목록을 받는다) ─────────────

    @GetMapping(value = "/a/v1/availability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> availabilityA(
            @RequestParam String hotelCodes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults,
            @RequestParam(defaultValue = "0") int children) {
        return switch (modeOf("a")) {
            case ERROR -> ResponseEntity.status(503)
                    .body("""
                            {"error":"SERVICE_UNAVAILABLE","message":"temporarily unavailable"}""");
            case NO_RESPONSE -> noResponse();
            default -> ResponseEntity.ok(availability(checkIn, checkOut));
        };
    }

    @GetMapping(value = "/b/api/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchB(
            @RequestParam String propertyIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults,
            @RequestParam(defaultValue = "0") int children) {
        return switch (modeOf("b")) {
            // B는 장애 상황에서도 HTTP 200이고 본문 코드로 알린다.
            case ERROR -> ResponseEntity.ok("""
                    {"resultCode":"E503","resultMessage":"TEMPORARILY_UNAVAILABLE","data":null}""");
            case NO_RESPONSE -> noResponse();
            default -> ResponseEntity.ok(search(checkIn, checkOut));
        };
    }

    private String modeOf(String supplier) {
        return modes.getOrDefault(supplier, NORMAL);
    }

    private static ResponseEntity<String> noResponse() {
        try {
            Thread.sleep(NO_RESPONSE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ResponseEntity.ok("{}");
    }

    // 날짜는 요청 기간(체크인일 ~ 체크아웃 전날)을 그대로 돌려준다. 고정 날짜를 박으면 요청 기간의 날짜가 응답에 없어
    // 모든 항목이 정규화에서 버려진다.
    private static String availability(LocalDate checkIn, LocalDate checkOut) {
        return """
                {
                  "items": [
                %s,
                %s
                  ]
                }
                """.formatted(
                aItem("A-10023", "Riverside Hotel Seoul", "DLX-TWN", "Deluxe Twin", A_10023_RATES, checkIn, checkOut),
                aItem("A-10044", "Namsan Garden Stay", "STD-DBL", "Standard Double", A_10044_RATES, checkIn, checkOut));
    }

    private static String aItem(String hotelCode, String hotelName, String roomTypeCode, String roomTypeName,
                                int[][] rates, LocalDate checkIn, LocalDate checkOut) {
        StringJoiner dailyRates = new StringJoiner(",\n          ", "\n          ", "\n        ");
        int night = 0;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            int[] rate = rates[night++ % rates.length];
            dailyRates.add("""
                    { "date": "%s", "remainingRooms": %d, "nightlyRate": %d, "taxAmount": %d }"""
                    .formatted(date, rate[0], rate[1], rate[2]));
        }
        return """
                    {
                      "hotelCode": "%s",
                      "hotelName": "%s",
                      "roomTypeCode": "%s",
                      "roomTypeName": "%s",
                      "maxOccupancy": 2,
                      "breakfastIncluded": false,
                      "currency": "KRW",
                      "dailyRates": [%s]
                    }""".formatted(hotelCode, hotelName, roomTypeCode, roomTypeName, dailyRates);
    }

    private static String search(LocalDate checkIn, LocalDate checkOut) {
        StringJoiner inventory = new StringJoiner(",\n                ", "\n                ", "\n              ");
        int night = 0;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            inventory.add("""
                    { "date": "%s", "remainingRooms": %d }"""
                    .formatted(date, B_77120_REMAINING[night++ % B_77120_REMAINING.length]));
        }
        return """
                {
                  "resultCode": "0000",
                  "resultMessage": "SUCCESS",
                  "data": {
                    "items": [
                      {
                        "propertyId": "B77120",
                        "propertyName": "Riverside Hotel Seoul",
                        "roomId": "R-401",
                        "roomName": "Deluxe Twin Room",
                        "maxOccupancy": 2,
                        "breakfastIncluded": true,
                        "currency": "KRW",
                        "totalPrice": 452000,
                        "taxIncluded": true,
                        "inventory": [%s]
                      }
                    ]
                  }
                }
                """.formatted(inventory);
    }

    private static final String A_HOTELS = """
            {
              "items": [
                {
                  "hotelCode": "A-10023",
                  "hotelName": "Riverside Hotel Seoul",
                  "roomTypes": [
                    { "roomTypeCode": "DLX-TWN", "roomTypeName": "Deluxe Twin", "maxOccupancy": 2 }
                  ]
                },
                {
                  "hotelCode": "A-10044",
                  "hotelName": "Namsan Garden Stay",
                  "roomTypes": [
                    { "roomTypeCode": "STD-DBL", "roomTypeName": "Standard Double", "maxOccupancy": 2 }
                  ]
                }
              ]
            }
            """;

    private static final String B_PROPERTIES = """
            {
              "resultCode": "0000",
              "resultMessage": "SUCCESS",
              "data": {
                "items": [
                  {
                    "propertyId": "B77120",
                    "propertyName": "Riverside Hotel Seoul",
                    "rooms": [
                      { "roomId": "R-401", "roomName": "Deluxe Twin Room", "maxOccupancy": 2 }
                    ]
                  }
                ]
              }
            }
            """;
}
