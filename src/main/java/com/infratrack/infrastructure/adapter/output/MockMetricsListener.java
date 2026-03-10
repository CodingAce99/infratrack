package com.infratrack.infrastructure.adapter.output;

import com.infratrack.domain.event.AssetCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockMetricsListener {

    private static final Logger log = LoggerFactory.getLogger(MockMetricsListener.class);

    // This is a mock listener for demonstration purposes.
    // Demo/dev profile: generates simulated metrics on asset creation.
    @EventListener
    public void onAssetCreated(AssetCreatedEvent event) {
        double cpu = ThreadLocalRandom.current().nextDouble(5.0, 45.0);
        double memory = ThreadLocalRandom.current().nextDouble(40.0, 80.0);
        double disk = ThreadLocalRandom.current().nextDouble(20.0, 70.0);
        log.info("MockMetricsListener received AssetCreatedEvent: assetId={}, cpuUsage={}%, memoryUsage={}%, diskUsage={}%",
                event.assetId(), String.format("%.2f", cpu), String.format("%.2f", memory), String.format("%.2f", disk));
    }
}
