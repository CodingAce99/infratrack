package com.infratrack.infrastructure.adapter.input;

import com.infratrack.application.port.input.MonitorAssetUseCase;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

@DisplayName("MetricsScheduler — observability instrumentation")
@ExtendWith(MockitoExtension.class)
class MetricsSchedulerTest {

    @Mock
    private MonitorAssetUseCase monitorUseCase;

    private SimpleMeterRegistry meterRegistry;
    private MetricsScheduler scheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduler = new MetricsScheduler(monitorUseCase, meterRegistry);
    }

    @Nested
    @DisplayName("Timer: infratrack.monitoring.collection.duration")
    class CollectionDurationTimer {

        @Test
        @DisplayName("should record one timer sample on successful sweep")
        void recordsTimerOnSuccess() {
            scheduler.collectAll();

            Timer timer = meterRegistry.find("infratrack.monitoring.collection.duration").timer();
            assertNotNull(timer, "timer should be registered");
            assertEquals(1, timer.count(), "one sweep → one timer sample");
            assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) > 0,
                    "duration should be positive");
        }

        @Test
        @DisplayName("should record timer sample even when sweep fails")
        void recordsTimerOnFailure() {
            doThrow(new RuntimeException("sweep failed"))
                    .when(monitorUseCase).collectAllActive();

            assertThrows(RuntimeException.class, () -> scheduler.collectAll());

            Timer timer = meterRegistry.find("infratrack.monitoring.collection.duration").timer();
            assertNotNull(timer, "timer should be registered even on failure");
            assertEquals(1, timer.count(), "failed sweep still records one sample");
            assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) > 0,
                    "duration should be positive even on failure");
        }
    }
}
