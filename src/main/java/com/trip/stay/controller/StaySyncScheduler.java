package com.trip.stay.controller;

import com.trip.stay.business.StayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stay.sync.enabled", havingValue = "true", matchIfMissing = true)
public class StaySyncScheduler implements ApplicationRunner {

    private final StayService stayService;

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
            stayService.syncAll();
        } catch (RuntimeException e) {
            log.error("[숙소 동기화 : 실행 실패]: trigger={}", trigger, e);
        }
    }
}
