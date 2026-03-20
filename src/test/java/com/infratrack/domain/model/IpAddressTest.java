package com.infratrack.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressTest {

    @Test
    void shouldCreateValidIpv4Address() {
        IpAddress ip = IpAddress.of("192.168.1.1");

        assertEquals("192.168.1.1", ip.getValue());
    }

    @Test
    void shouldRejectNullIpAddress() {
        assertThrows(NullPointerException.class, () -> {
            IpAddress.of(null);
        });
    }

    @Test
    void shouldRejectEmptyIpAddress() {
        assertThrows(IllegalArgumentException.class, () -> {
            IpAddress.of("");
        });
    }

    @Test
    void shouldRejectInvalidIpAddress() {
        assertThrows(IllegalArgumentException.class, () -> {
            IpAddress.of("256.256.256.256!");
        });
    }

    @Test
    void shouldAcceptIpv4BoundaryValues() {
        assertDoesNotThrow(() -> IpAddress.of("0.0.0.0"));
        assertDoesNotThrow(() -> IpAddress.of("255.255.255.255"));
    }

    @Test
    void shouldRejectHostnameStartingWithHyphen() {
        assertThrows(IllegalArgumentException.class, () ->
                IpAddress.of("-invalid-start"));
    }

    @Test
    void shouldRejectHostnameEndingWithHyphen() {
        assertThrows(IllegalArgumentException.class, () ->
                IpAddress.of("server-01-"));
    }

    @Test
    void shouldAcceptDockerHostname() {
        IpAddress ip = IpAddress.of("web-server-01");
        assertEquals("web-server-01", ip.getValue());
    }

    @Test
    void shouldAcceptSingleLabelHostname() {
        IpAddress ip = IpAddress.of("localhost");
        assertEquals("localhost", ip.getValue());
    }

    @Test
    void twoIpAddressesWithSameValueShouldBeEqual() {
        IpAddress ip1 = IpAddress.of("192.168.1.1");
        IpAddress ip2 = IpAddress.of("192.168.1.1");

        assertEquals(ip1, ip2);
        assertEquals(ip1.hashCode(), ip2.hashCode());
    }
}