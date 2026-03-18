package com.infratrack.infrastructure.adapter.input;


import com.infratrack.application.port.input.MonitorAssetUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MetricsScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricsScheduler.class);

    private final MonitorAssetUseCase monitorUseCase;

    public MetricsScheduler(MonitorAssetUseCase monitorUseCase) {
        this.monitorUseCase = Objects.requireNonNull(monitorUseCase, "MonitorAssetUseCase cannot be null");
    }

    @Scheduled(fixedRateString = "${infratrack.monitoring.interval-seconds:60}000")
    public void collectAll() {
        log.info("Scheduled metrics collection started");
        monitorUseCase.collectAllActive();
    }
}
