package com.infratrack.domain.model;

public class DuplicateIpAddressException extends RuntimeException {

    public DuplicateIpAddressException(IpAddress ipAddress) {
        super("Asset with IP address '" + ipAddress.getValue() + "' already exists");
    }
}
