package com.infratrack.infrastructure.adapter.output;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SshMetricsCollector — output parsing")
class SshMetricsCollectorTest {

    @Nested
    @DisplayName("parseCpuUsage()")
    class ParseCpuUsageTest {

        @Test
        @DisplayName("should parse CPU when idle is attached to previous token")
        void shouldParseCpu_whenIdleAttached() {
            String line = "%Cpu(s):  0.0 us,  0.0 sy,  0.0 ni,100.0 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st";
            double result = SshMetricsCollector.parseCpuUsage(line);
            assertEquals(0.0, result, 0.1);
        }

        @Test
        @DisplayName("should parse CPU when idle is separated by space")
        void shouldParseCpu_whenIdleSeparated() {
            String line = "%Cpu(s):  5.2 us,  3.1 sy,  0.0 ni, 91.7 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st";
            double result = SshMetricsCollector.parseCpuUsage(line);
            assertEquals(8.3, result, 0.1);
        }

        @Test
        @DisplayName("should throw when line does not contain idle field")
        void shouldThrow_whenLineIsUnrecognized() {
            assertThrows(IllegalArgumentException.class,
                    () -> SshMetricsCollector.parseCpuUsage("garbage line"));
        }
    }

    @Nested
    @DisplayName("parseMemoryUsage()")
    class ParseMemoryUsageTest {

        @Test
        @DisplayName("should parse MemoryUsage when input is a String of two separated numbers")
        void shouldParseMemoryUsage_whenInputIsAStringOfTwoNumbers() {
            String input = "883 15954";
            double result = SshMetricsCollector.parseMemoryUsage(input);
            assertEquals(5.53, result, 0.1);
        }

        @Test
        @DisplayName("should throw when input cannot be parsed")
        void shouldThrow_whenInputIsNotValid() {
            assertThrows(NumberFormatException.class,
                    () -> SshMetricsCollector.parseMemoryUsage("invalid input"));
        }
    }

    @Nested
    @DisplayName("parseDiskUsage()")
    class ParseDiskUsageTest {

        @Test
        @DisplayName("should parse DiskUsage when input is a String with a percentage")
        void shouldParseDiskUsage_whenInputIsAStringWithAPercentage() {
            String input = "1%";
            double result = SshMetricsCollector.parseDiskUsage(input);
            assertEquals(1.0, result, 0.1);
        }

        @Test
        @DisplayName("should throw when input cannot be parsed")
        void shouldThrow_whenInputIsNotValid() {
            assertThrows(NumberFormatException.class,
                    () -> SshMetricsCollector.parseDiskUsage("invalid input"));
        }
    }
}
