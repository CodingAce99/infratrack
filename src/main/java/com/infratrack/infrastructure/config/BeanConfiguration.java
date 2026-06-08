package com.infratrack.infrastructure.config;

import com.infratrack.application.port.input.AuthenticateUserUseCase;
import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.application.port.input.MonitorAssetUseCase;
import com.infratrack.application.port.output.*;
import com.infratrack.application.service.AssetService;
import com.infratrack.application.service.AuthenticationService;
import com.infratrack.application.service.MonitoringService;
import com.infratrack.infrastructure.adapter.output.*;
import com.infratrack.infrastructure.persistence.SpringDataAssetRepository;
import com.infratrack.infrastructure.persistence.SpringDataMetricSnapshotRepository;
import com.infratrack.infrastructure.persistence.SpringDataUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.infratrack.application.port.output.PasswordEncoder;
import com.infratrack.application.port.output.UserRepository;
import com.infratrack.infrastructure.adapter.output.BCryptPasswordEncoderAdapter;
import com.infratrack.infrastructure.adapter.output.JpaUserRepository;

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

    // --- User beans ---

    @Bean
    @Profile({"demo", "prod"})
    public UserRepository jpaUserRepository(SpringDataUserRepository springRepo) {
        return new JpaUserRepository(springRepo);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    @Profile({"demo", "prod"})
    public TokenGenerator jjwtTokenGenerator(
            @Value("${infratrack.jwt.secret}") String secret,
            @Value("${infratrack.jwt.expiration-ms}") long expirationMs) {
        return new JjwtTokenGenerator(secret, expirationMs);
    }

    @Bean
    @Profile({"demo", "prod"})
    public AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenGenerator tokenGenerator) {
        return new AuthenticationService(userRepository, passwordEncoder, tokenGenerator);
    }
}

