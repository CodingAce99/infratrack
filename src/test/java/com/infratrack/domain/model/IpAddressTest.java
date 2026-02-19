package com.infratrack.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressTest {

    @Test
    void shouldCreateValidIpv4Address() {
        // Arrange & Act
        IpAddress ip = new IpAddress("192.168.1.1");

        // Assert
        assertEquals("192.168.1.1", ip.value());
    }

    @Test
    void shouldRejectNullIpAddress() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new IpAddress(null);
        });
    }

    @Test
    void shouldRejectEmptyIpAddress() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new IpAddress("");
        });
    }

    @Test
    void shouldRejectInvalidIpAddress() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new IpAddress("999.999.999.999");
        });
    }

    @Test
    void shouldRejectNonNumericIpAddress() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new IpAddress("not.an.ip.address");
        });
    }

    @Test
    void twoIpAddressesWithSameValueShouldBeEqual() {
        // Arrange
        IpAddress ip1 = new IpAddress("192.168.1.1");
        IpAddress ip2 = new IpAddress("192.168.1.1");

        // Act & Assert
        assertEquals(ip1, ip2);
        assertEquals(ip1.hashCode(), ip2.hashCode());
    }
}