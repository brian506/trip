package com.trip.config.supplier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService supplierExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
