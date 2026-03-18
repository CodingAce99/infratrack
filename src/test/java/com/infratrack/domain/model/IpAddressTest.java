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
            IpAddress.of("999.999.999.999");
        });
    }

    @Test
    void shouldRejectNonNumericIpAddress() {
        assertThrows(IllegalArgumentException.class, () -> {
            IpAddress.of("not.an.ip.address");
        });
    }

    @Test
    void twoIpAddressesWithSameValueShouldBeEqual() {
        IpAddress ip1 = IpAddress.of("192.168.1.1");
        IpAddress ip2 = IpAddress.of("192.168.1.1");

        assertEquals(ip1, ip2);
        assertEquals(ip1.hashCode(), ip2.hashCode());
    }
}