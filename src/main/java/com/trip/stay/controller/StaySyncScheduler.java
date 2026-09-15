package com.trip.stay.controller;

import com.trip.stay.business.StaySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 동기화 진입점. 기동 시 1회, 이후 매일 새벽(stay.sync.cron)에 돈다.
// 숙소 목록은 자주 바뀌지 않으므로 검색 트래픽과 무관한 고정 주기로 갱신한다.
// 기동 시 공급사가 죽어 있어도 기동은 계속된다. 매핑이 비면 검색은 빈 결과를 돌려준다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stay.sync.enabled", havingValue = "true", matchIfMissing = true)
public class StaySyncScheduler implements ApplicationRunner {

    private final StaySyncService staySyncService;

    @Override
    public void run(ApplicationArguments args) {
        runSafely("기동");
    }

    @Scheduled(cron = "${stay.sync.cron}")
    public void scheduled() {
        runSafely("스케줄");
    }

    private void runSafely(String trigger) {
        try {
            staySyncService.syncAll();
        } catch (RuntimeException e) {
            log.error("숙소 목록 동기화 실패. trigger={}", trigger, e);
        }
    }
}
