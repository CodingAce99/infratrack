package com.infratrack.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressTest {

    @Test
    void shouldCreateValidIpv4Address() {
        // Arrange & Act
        IpAddress ip = IpAddress.of("192.168.1.1");

        // Assert
        assertEquals("192.168.1.1", ip.getValue());
    }

    @Test
    void shouldRejectNullIpAddress() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            IpAddress.of(null);
        });
    }

    @Test
    void shouldRejectEmptyIpAddress() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            IpAddress.of("");
        });
    }

    @Test
    void shouldRejectInvalidIpAddress() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            IpAddress.of("999.999.999.999");
        });
    }

    @Test
    void shouldRejectNonNumericIpAddress() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            IpAddress.of("not.an.ip.address");
        });
    }

    @Test
    void twoIpAddressesWithSameValueShouldBeEqual() {
        // Arrange
        IpAddress ip1 = IpAddress.of("192.168.1.1");
        IpAddress ip2 = IpAddress.of("192.168.1.1");

        // Act & Assert
        assertEquals(ip1, ip2);
        assertEquals(ip1.hashCode(), ip2.hashCode());
    }
}