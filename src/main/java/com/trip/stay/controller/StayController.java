package com.trip.stay.controller;

import com.trip.stay.business.StayService;
import com.trip.stay.controller.request.StaySearchRequest;
import com.trip.stay.controller.response.StaySearchResponse;
import com.trip.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stay", description = "숙소 검색과 목록 동기화")
@RestController
@RequestMapping("/api/v1/stays")
@RequiredArgsConstructor
public class StayController {

    private final StayService stayService;

    @Operation(
            summary = "통합 검색",
            description = "날짜와 인원으로 DB에서 먼저 숙소 코드를 조회하고, 이걸 가지고 공급사에 요금과 재고를 요청한다."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<StaySearchResponse>> search(@Valid @ModelAttribute StaySearchRequest request) {
        StaySearchResponse result = stayService.search(request.toPeriod(), request.toGuests());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "숙소 목록 동기화 실행",
            description = "모든 공급사의 숙소 목록을 받아 DB에 저장한다."
    )
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Void>> sync() {
        stayService.syncAll();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }
}
