package com.trip.mock.supplier;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 공급사 A·B를 흉내 내는 Mock. supplier 프로파일로 9090 포트에 별도 프로세스로 띄운다.
// 고정 응답을 준다.
@Profile("supplier")
@RestController
public class MockSupplierController {

    // ── ① 숙소 목록 (정적 콘텐츠) ─────────────────────────────

    @GetMapping(value = "/a/v1/hotels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> hotelsA() {
        return ResponseEntity.ok(A_HOTELS);
    }

    @GetMapping(value = "/b/api/properties", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> propertiesB() {
        return ResponseEntity.ok(B_PROPERTIES);
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
