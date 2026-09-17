package com.trip.stay.controller;

import com.trip.stay.controller.response.StaySearchResponse;
import com.trip.supplier.global.SupplierProperties;
import com.trip.support.ApiTest;
import com.trip.support.fixture.StayPeriodFixture;
import com.trip.support.response.ApiResponse;
import com.trip.support.vo.StayPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 검색은 2박으로 고정한다. 공급사 응답 픽스처의 요금·재고가 같은 2박을 채운다.
abstract class StayApiTest extends ApiTest {

    protected static final StayPeriod PERIOD = StayPeriodFixture.twoNights();

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    protected SupplierProperties supplierProperties;

    protected void sync() throws Exception {
        mockMvc.perform(post("/api/v1/stays/sync")).andExpect(status().isCreated());
    }

    protected ResultActions search(int adults) throws Exception {
        return mockMvc.perform(get("/api/v1/stays/search")
                .param("checkIn", PERIOD.checkIn().toString())
                .param("checkOut", PERIOD.checkOut().toString())
                .param("adults", String.valueOf(adults)));
    }

    protected StaySearchResponse searchResult(int adults) throws Exception {
        String body = search(adults)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return jsonMapper.readValue(body, new TypeReference<ApiResponse<StaySearchResponse>>() {
        }).data();
    }
}
