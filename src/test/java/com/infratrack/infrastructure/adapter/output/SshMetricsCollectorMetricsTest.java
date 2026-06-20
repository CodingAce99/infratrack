package com.infratrack.infrastructure.adapter.output;

import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetType;
import com.infratrack.domain.model.Credentials;
import com.infratrack.domain.model.IpAddress;
import com.infratrack.domain.model.MetricSnapshot;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SshMetricsCollector — observability instrumentation")
class SshMetricsCollectorMetricsTest {

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    private Asset testAsset() {
        return Asset.create(
                "Test Server",
                AssetType.SERVER,
                IpAddress.of("127.0.0.1"),
                Credentials.of("nobody", "nopass")
        );
    }

    /**
     * Test seam: SshMetricsCollector subclass that bypasses real SSH by
     * overriding {@code doCollect()} to return a synthetic snapshot.
     * This lets us verify the success counter path without a running SSH server.
     */
    private SshMetricsCollector successCollector() {
        return new SshMetricsCollector(22, meterRegistry) {
            @Override
            protected MetricSnapshot doCollect(Asset asset) throws IOException {
                return MetricSnapshot.of(asset.getId(), 50.0, 60.0, 70.0);
            }
        };
    }

    /**
     * Test seam: SshMetricsCollector subclass that always fails collection.
     * Overrides {@code doCollect()} to throw IOException, simulating an
     * unreachable SSH server. This replaces the environment-sensitive
     * localhost:22 assumption with a deterministic failure path.
     */
    private SshMetricsCollector failureCollector() {
        return new SshMetricsCollector(22, meterRegistry) {
            @Override
            protected MetricSnapshot doCollect(Asset asset) throws IOException {
                throw new IOException("simulated SSH connection failure");
            }
        };
    }

    /**
     * Test seam: SshMetricsCollector subclass that throws an unchecked
     * exception during collection, simulating a parse/programming failure
     * (e.g. {@link IllegalArgumentException} from malformed command output).
     */
    private SshMetricsCollector uncheckedFailureCollector() {
        return new SshMetricsCollector(22, meterRegistry) {
            @Override
            protected MetricSnapshot doCollect(Asset asset) throws IOException {
                throw new IllegalArgumentException("simulated parse failure");
            }
        };
    }

    @Nested
    @DisplayName("Counter: infratrack.ssh.collection")
    class SshCollectionCounter {

        @Test
        @DisplayName("should increment success counter when SSH collection succeeds")
        void incrementsSuccessCounterOnSshSuccess() {
            SshMetricsCollector collector = successCollector();
            Asset asset = testAsset();

            MetricSnapshot result = collector.collect(asset);

            assertNotNull(result, "should return a metric snapshot on success");
            assertEquals(50.0, result.cpuUsage());
            assertEquals(60.0, result.memoryUsage());
            assertEquals(70.0, result.diskUsage());

            Counter successCounter = meterRegistry.find("infratrack.ssh.collection")
                    .tag("outcome", "success")
                    .counter();
            assertNotNull(successCounter, "success counter should be registered");
            assertEquals(1.0, successCounter.count(),
                    "one successful collection → one success counter increment");
        }

        @Test
        @DisplayName("should increment failure counter when SSH collection fails")
        void incrementsFailureCounterOnSshFailure() {
            SshMetricsCollector collector = failureCollector();
            Asset asset = testAsset();
            try {
                collector.collect(asset);
                fail("should have thrown — doCollect() throws IOException");
            } catch (RuntimeException e) {
                // expected — doCollect() failure seam throws IOException,
                // which is wrapped as RuntimeException by collect()
            }

            Counter failureCounter = meterRegistry.find("infratrack.ssh.collection")
                    .tag("outcome", "failure")
                    .counter();
            assertNotNull(failureCounter, "failure counter should be registered");
            assertEquals(1.0, failureCounter.count(),
                    "one failed collection → one failure counter increment");
        }

        @Test
        @DisplayName("should not increment success counter when SSH collection fails")
        void doesNotIncrementSuccessOnFailure() {
            SshMetricsCollector collector = failureCollector();
            Asset asset = testAsset();
            try {
                collector.collect(asset);
                fail("should have thrown");
            } catch (RuntimeException e) {
                // expected
            }

            Counter successCounter = meterRegistry.find("infratrack.ssh.collection")
                    .tag("outcome", "success")
                    .counter();
            // The success counter may not be registered if never incremented.
            if (successCounter != null) {
                assertEquals(0.0, successCounter.count(),
                        "success counter should not be incremented on failure");
            }
        }

        @Test
        @DisplayName("should increment failure counter when collection fails with unchecked exception")
        void incrementsFailureCounterOnUncheckedFailure() {
            SshMetricsCollector collector = uncheckedFailureCollector();
            Asset asset = testAsset();
            try {
                collector.collect(asset);
                fail("should have thrown — doCollect() throws IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // expected — unchecked exception should propagate unchanged
            }

            Counter failureCounter = meterRegistry.find("infratrack.ssh.collection")
                    .tag("outcome", "failure")
                    .counter();
            assertNotNull(failureCounter,
                    "failure counter should be registered even for unchecked collection exceptions");
            assertEquals(1.0, failureCounter.count(),
                    "one unchecked failure → one failure counter increment");
        }
    }
}
