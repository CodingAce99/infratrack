package com.infratrack.infrastructure.config;

import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.application.port.input.MonitorAssetUseCase;
import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.application.port.output.DomainEventPublisher;
import com.infratrack.application.port.output.MetricSnapshotRepository;
import com.infratrack.application.port.output.MetricsCollector;
import com.infratrack.application.service.AssetService;
import com.infratrack.application.service.MonitoringService;
import com.infratrack.infrastructure.adapter.output.*;
import com.infratrack.infrastructure.persistence.SpringDataAssetRepository;
import com.infratrack.infrastructure.persistence.SpringDataMetricSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class BeanConfiguration {

    @Bean
    @Profile("dev")
    public AssetRepository inMemoryAssetRepository() {
        return new InMemoryAssetRepository();
    }

    @Bean
    @Profile({"demo", "prod"})
    public AssetRepository jpaAssetRepository(SpringDataAssetRepository springRepo) {
        return new JpaAssetRepository(springRepo);
    }

    @Bean
    public ManageAssetUseCase manageAssetUseCase(AssetRepository assetRepository, DomainEventPublisher domainEventPublisher) {
        return new AssetService(assetRepository, domainEventPublisher);
    }

    // --- Monitoring beans ---

    @Bean
    @Profile("dev")
    public MetricSnapshotRepository inMemoryMetricSnapshotRepository() {
        return new InMemoryMetricSnapshotRepository();
    }

    @Bean
    @Profile({"demo", "prod"})
    public MetricSnapshotRepository jpaMetricSnapshotRepository(
            SpringDataMetricSnapshotRepository springRepo) {
        return new JpaMetricSnapshotRepository(springRepo);
    }

    @Bean
    public MonitorAssetUseCase monitorAssetUseCase(
            AssetRepository assetRepository,
            MetricsCollector metricsCollector,
            MetricSnapshotRepository metricSnapshotRepository) {
        return new MonitoringService(assetRepository, metricsCollector, metricSnapshotRepository);
    }

    @Bean
    @Profile("dev")
    public MetricsCollector mockMetricsCollector() {
        return new MockMetricsCollector();
    }

    @Bean
    @Profile({"demo", "prod"})
    public MetricsCollector sshMetricsCollector(
            @Value("${infratrack.ssh.port:22}") int sshPort) {
        return new SshMetricsCollector(sshPort);
    }
}

