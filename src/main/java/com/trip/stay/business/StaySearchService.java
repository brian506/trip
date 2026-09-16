package com.trip.stay.business;

import com.trip.stay.controller.response.StaySearchResponse;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StaySearchService {

    // 매핑 조회 → 공급사별 코드 묶기 → 병렬 호출 → 정규화 → 결과 조립 순으로 구현한다.
    // 다만 공급사가 전부 실패하면 부분 성공이 아니므로 잡지 않고 올려 502를 낸다.
    public StaySearchResponse search(StayPeriod period, Guests guests) {
        return new StaySearchResponse(List.of(), List.of());
    }
}
