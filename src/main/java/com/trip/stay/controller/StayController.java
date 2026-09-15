package com.trip.stay.controller;

import com.trip.stay.business.StaySyncService;
import com.trip.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stay", description = "숙소 목록 동기화")
@RestController
@RequestMapping("/api/v1/stays")
@RequiredArgsConstructor
public class StayController {

    private final StaySyncService staySyncService;

    @Operation(
            summary = "숙소 목록 동기화 실행",
            description = "모든 공급사의 숙소 목록을 받아 매핑을 갱신한다. 실패한 공급사는 건너뛰고 로그에 남긴다."
    )
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Void>> sync() {
        staySyncService.syncAll();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }
}
