package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.MetricsCollector;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.IpAddress;
import com.infratrack.domain.model.MetricSnapshot;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SshMetricsCollector implements MetricsCollector {

    private final int sshPort;

    public SshMetricsCollector(int sshPort) {
        this.sshPort = sshPort;
    }

    @Override
    public MetricSnapshot collect(Asset asset) {
        IpAddress ip = asset.getIpAddress();
        SSHClient ssh = new SSHClient();
        try {
            ssh.addHostKeyVerifier(new PromiscuousVerifier()); // no host key verification - acceptable for demo
            ssh.connect(ip.getValue(), sshPort);
            ssh.authPassword(
                    asset.getCredentials().getUsername(),
                    asset.getCredentials().getPassword()
            );
            try (Session session = ssh.startSession()) {

                Session.Command cpuCmd = session.exec("top -bn1 | grep '%Cpu'");
                String cpuOutput = new String(cpuCmd.getInputStream().readAllBytes());
                cpuCmd.join(5, TimeUnit.SECONDS); // wait for remote process to finish

                Session.Command memCmd = session.exec("free -m | awk 'NR==2 {printf \"%d %d\", $3, $2}'");
                String memOutput = new String(memCmd.getInputStream().readAllBytes());
                memCmd.join(5, TimeUnit.SECONDS);

                Session.Command diskCmd = session.exec("df / | awk 'NR==2 {print $5}'");
                String diskOutput = new String(diskCmd.getInputStream().readAllBytes());
                diskCmd.join(5, TimeUnit.SECONDS);

                return MetricSnapshot.of(
                        asset.getId(),
                        parseCpuUsage(cpuOutput),
                        parseMemoryUsage(memOutput),
                        parseDiskUsage(diskOutput)
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "SSH collection failed for asset " + asset.getId().getValue(), e);
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                // ignored - already in cleanup
            }

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