package com.infratrack.infrastructure.adapter.input;

import com.infratrack.application.port.input.MonitorAssetUseCase;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.MetricSnapshot;
import com.infratrack.infrastructure.adapter.input.dto.MetricSnapshotResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@RestController
@RequestMapping("/api/v1/assets")
public class MetricsRestController {

    private final MonitorAssetUseCase monitorUseCase;

    public MetricsRestController(MonitorAssetUseCase monitorUseCase) {
        this.monitorUseCase = Objects.requireNonNull(monitorUseCase, "MonitorAssetUseCase cannot be null");
    }

    @GetMapping("{id}/metrics")
    public ResponseEntity<MetricSnapshotResponse> getLatestMetrics(@PathVariable String id) {
        List<MetricSnapshot> snapshots = monitorUseCase.getHistory(AssetId.of(id), 1);
        return snapshots.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(MetricSnapshotResponse.from(snapshots.get(0)));
    }

    @GetMapping("/{id}/metrics/history")
    public ResponseEntity<List<MetricSnapshotResponse>> getMetricsHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        List<MetricSnapshot> history = monitorUseCase.getHistory(AssetId.of(id), limit);
        return ResponseEntity.ok(
                history.stream().map(MetricSnapshotResponse::from).toList()
        );
    }
}
