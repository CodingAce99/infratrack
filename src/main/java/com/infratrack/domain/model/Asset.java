package com.infratrack.domain.model;

import java.util.Objects;

public final class Asset {

    private final AssetId id;
    private final String name;
    private final AssetType type;
    private IpAddress ipAddress;
    private AssetStatus status;
    private Credentials credentials;

    private Asset(AssetId id, String name, AssetType type, IpAddress ipAddress, AssetStatus status, Credentials credentials) {
        this.id          = Objects.requireNonNull(id,          "Asset id cannot be null");
        this.name        = Objects.requireNonNull(name,        "Asset name cannot be null");
        this.type        = Objects.requireNonNull(type,        "Asset type cannot be null");
        this.ipAddress   = Objects.requireNonNull(ipAddress,   "Asset ipAddress cannot be null");
        this.status      = Objects.requireNonNull(status,      "Asset status cannot be null");
        this.credentials = Objects.requireNonNull(credentials, "Asset credentials cannot be null");
    }

    // --- Factory methods ---

    public static Asset create(String name, AssetType type, IpAddress ipAddress, Credentials credentials) {
        return new Asset(AssetId.generate(), name, type, ipAddress, AssetStatus.ACTIVE, credentials);
    }

    public static Asset reconstitute(AssetId id, String name, AssetType type, IpAddress ipAddress, AssetStatus status, Credentials credentials) {
        return new Asset(id, name, type, ipAddress, status, credentials);
    }

    // --- Behaviour methods ---

    public void activate() {
        this.status = AssetStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = AssetStatus.INACTIVE;
    }

    public void putInMaintenance() {
        this.status = AssetStatus.MAINTENANCE;
    }

    public void updateCredentials(Credentials newCredentials) {
        this.credentials = Objects.requireNonNull(newCredentials, "New credentials cannot be null");
    }

    public void updateIpAddress(IpAddress newIpAddress) {
        this.ipAddress = Objects.requireNonNull(newIpAddress, "New ipAddress cannot be null");
    }

    // --- Getters ---

    public AssetId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AssetType getType() {
        return type;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    // --- Standard methods ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Asset{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", ipAddress=" + ipAddress +
                '}';
    }
}

