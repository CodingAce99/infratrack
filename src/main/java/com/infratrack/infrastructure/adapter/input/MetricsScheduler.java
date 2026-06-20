package com.infratrack.infrastructure.adapter.input;


import com.infratrack.application.port.input.MonitorAssetUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MetricsScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricsScheduler.class);

    private final MonitorAssetUseCase monitorUseCase;
    private final MeterRegistry meterRegistry;

    public MetricsScheduler(MonitorAssetUseCase monitorUseCase, MeterRegistry meterRegistry) {
        this.monitorUseCase = Objects.requireNonNull(monitorUseCase, "MonitorAssetUseCase cannot be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry cannot be null");
    }

    @Scheduled(fixedRateString = "${infratrack.monitoring.interval-seconds:60}000")
    public void collectAll() {
        log.info("Scheduled metrics collection started");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            monitorUseCase.collectAllActive();
        } finally {
            sample.stop(Timer.builder("infratrack.monitoring.collection.duration")
                    .description("Duration of scheduled monitoring sweeps")
                    .register(meterRegistry));
        }
    }
}
