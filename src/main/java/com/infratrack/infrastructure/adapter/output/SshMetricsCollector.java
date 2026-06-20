package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.MetricsCollector;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.IpAddress;
import com.infratrack.domain.model.MetricSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SshMetricsCollector implements MetricsCollector {

    private final int sshPort;
    private final MeterRegistry meterRegistry;

    public SshMetricsCollector(int sshPort, MeterRegistry meterRegistry) {
        this.sshPort = sshPort;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Test seam: performs the full SSH collection (connect, authenticate, execute
     * commands, parse, disconnect) for a single asset. Override in test subclasses
     * to simulate successful collection without a real SSH server.
     * <p>
     * The {@link #collect(Asset)} method wraps this with counter instrumentation
     * — test subclasses only need to return a synthetic {@link MetricSnapshot}.
     */
    protected MetricSnapshot doCollect(Asset asset) throws IOException {
        IpAddress ip = asset.getIpAddress();
        SSHClient ssh = new SSHClient();
        try {
            ssh.addHostKeyVerifier(new PromiscuousVerifier()); // no host key verification - acceptable for demo
            ssh.connect(ip.getValue(), sshPort);
            ssh.authPassword(
                    asset.getCredentials().getUsername(),
                    asset.getCredentials().getPassword()
            );

            String cpuOutput;
            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec("top -bn1 | grep '%Cpu'");
                cpuOutput = new String(cmd.getInputStream().readAllBytes());
                cmd.join(5, TimeUnit.SECONDS);
            }

            String memOutput;
            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec("free -m | awk 'NR==2 {printf \"%d %d\", $3, $2}'");
                memOutput = new String(cmd.getInputStream().readAllBytes());
                cmd.join(5, TimeUnit.SECONDS);
            }

            String diskOutput;
            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec("df / | awk 'NR==2 {print $5}'");
                diskOutput = new String(cmd.getInputStream().readAllBytes());
                cmd.join(5, TimeUnit.SECONDS);
            }

            return MetricSnapshot.of(
                    asset.getId(),
                    parseCpuUsage(cpuOutput),
                    parseMemoryUsage(memOutput),
                    parseDiskUsage(diskOutput)
            );
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                // /* ignored */
            }
        }
    }

    @Override
    public MetricSnapshot collect(Asset asset) {
        try {
            MetricSnapshot snapshot = doCollect(asset);

            meterRegistry.counter("infratrack.ssh.collection",
                    "outcome", "success").increment();

            return snapshot;
        } catch (IOException e) {
            meterRegistry.counter("infratrack.ssh.collection",
                    "outcome", "failure").increment();
            throw new RuntimeException(
                    "SSH collection failed for asset " + asset.getId().getValue(), e);
        } catch (RuntimeException e) {
            meterRegistry.counter("infratrack.ssh.collection",
                    "outcome", "failure").increment();
            throw e;
        }
    }

    static double parseCpuUsage(String topLine) {
        // Format: "%Cpu(s):  0.0 us,  0.0 sy,  0.0 ni,100.0 id, ..."
        // The idle field may be attached to the previous token (ni,100.0) or separated (ni, 100.0)
        // so we use regex to find the number before "id" instead of relying on fixed positions
        Pattern pattern = Pattern.compile("([\\d.]+)\\s*id");
        Matcher matcher = pattern.matcher(topLine);
        if (matcher.find()) {
            double idle = Double.parseDouble(matcher.group(1));
            return Math.round((100.0 - idle) * 10.0) / 10.0;
        }
        throw new IllegalArgumentException("Cannot parse CPU from: " + topLine);
    }

    static double parseMemoryUsage(String memLine) {

        String[] parts = memLine.split(" ");
        double used = Double.parseDouble(parts[0]);
        double total = Double.parseDouble(parts[1]);
        return Math.round((used / total) * 1000.0) / 10.0;
    }

    static double parseDiskUsage(String diskLine) {
        return Math.round(Double.parseDouble(diskLine.replace("%", "")) * 10.0) / 10.0;
    }
}
