package com.infratrack.application.port.output;

import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.MetricSnapshot;

public interface MetricsCollector {

    MetricSnapshot collect(Asset asset);
}
