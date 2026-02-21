package com.infratrack.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Asset — domain model")
class AssetTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Asset createSampleAsset() {
        return Asset.create(
                "Core Router",
                AssetType.ROUTER,
                IpAddress.of("192.168.1.1"),
                Credentials.of("admin", "s3cr3t")
        );
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create() debe asignar status ACTIVE automáticamente")
    void create_shouldAssignActiveStatusAutomatically() {
        Asset asset = createSampleAsset();

        assertEquals(AssetStatus.ACTIVE, asset.getStatus());
    }

    @Test
    @DisplayName("create() debe generar un AssetId no nulo automáticamente")
    void create_shouldGenerateNonNullAssetId() {
        Asset asset = createSampleAsset();

        assertNotNull(asset.getId());
    }

    @Test
    @DisplayName("create() debe lanzar NullPointerException si name es null")
    void create_shouldThrowNullPointerException_whenNameIsNull() {
        assertThrows(NullPointerException.class, () ->
                Asset.create(null, AssetType.SERVER, IpAddress.of("10.0.0.1"), Credentials.of("u", "p"))
        );
    }

    @Test
    @DisplayName("create() debe lanzar NullPointerException si type es null")
    void create_shouldThrowNullPointerException_whenTypeIsNull() {
        assertThrows(NullPointerException.class, () ->
                Asset.create("Server A", null, IpAddress.of("10.0.0.1"), Credentials.of("u", "p"))
        );
    }

    @Test
    @DisplayName("create() debe lanzar NullPointerException si ipAddress es null")
    void create_shouldThrowNullPointerException_whenIpAddressIsNull() {
        assertThrows(NullPointerException.class, () ->
                Asset.create("Server A", AssetType.SERVER, null, Credentials.of("u", "p"))
        );
    }

    @Test
    @DisplayName("create() debe lanzar NullPointerException si credentials es null")
    void create_shouldThrowNullPointerException_whenCredentialsIsNull() {
        assertThrows(NullPointerException.class, () ->
                Asset.create("Server A", AssetType.SERVER, IpAddress.of("10.0.0.1"), null)
        );
    }

    // -------------------------------------------------------------------------
    // reconstitute()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("reconstitute() debe preservar exactamente todos los campos recibidos")
    void reconstitute_shouldPreserveAllFieldsExactly() {
        AssetId id          = AssetId.generate();
        String name         = "Edge Router";
        AssetType type      = AssetType.ROUTER;
        IpAddress ip        = IpAddress.of("172.16.0.1");
        AssetStatus status  = AssetStatus.MAINTENANCE;
        Credentials creds   = Credentials.of("ops", "p@ssw0rd");

        Asset asset = Asset.reconstitute(id, name, type, ip, status, creds);

        assertAll("todos los campos deben coincidir",
                () -> assertEquals(id,     asset.getId()),
                () -> assertEquals(name,   asset.getName()),
                () -> assertEquals(type,   asset.getType()),
                () -> assertEquals(ip,     asset.getIpAddress()),
                () -> assertEquals(status, asset.getStatus()),
                () -> assertEquals(creds,  asset.getCredentials())
        );
    }

    // -------------------------------------------------------------------------
    // Behaviour: status transitions
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("activate() debe cambiar el status a ACTIVE")
    void activate_shouldSetStatusToActive() {
        Asset asset = Asset.reconstitute(
                AssetId.generate(), "Server B", AssetType.SERVER,
                IpAddress.of("10.0.0.2"), AssetStatus.INACTIVE, Credentials.of("u", "p")
        );

        asset.activate();

        assertEquals(AssetStatus.ACTIVE, asset.getStatus());
    }

    @Test
    @DisplayName("deactivate() debe cambiar el status a INACTIVE")
    void deactivate_shouldSetStatusToInactive() {
        Asset asset = createSampleAsset();

        asset.deactivate();

        assertEquals(AssetStatus.INACTIVE, asset.getStatus());
    }

    @Test
    @DisplayName("putInMaintenance() debe cambiar el status a MAINTENANCE")
    void putInMaintenance_shouldSetStatusToMaintenance() {
        Asset asset = createSampleAsset();

        asset.putInMaintenance();

        assertEquals(AssetStatus.MAINTENANCE, asset.getStatus());
    }

    // -------------------------------------------------------------------------
    // Behaviour: updateCredentials()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateCredentials() debe actualizar las credenciales correctamente")
    void updateCredentials_shouldReplaceCredentials() {
        Asset asset = createSampleAsset();
        Credentials newCreds = Credentials.of("newuser", "newpass1");

        asset.updateCredentials(newCreds);

        assertEquals(newCreds, asset.getCredentials());
    }

    @Test
    @DisplayName("updateCredentials() debe lanzar NullPointerException si las nuevas credenciales son null")
    void updateCredentials_shouldThrowNullPointerException_whenNullIsProvided() {
        Asset asset = createSampleAsset();

        assertThrows(NullPointerException.class, () -> asset.updateCredentials(null));
    }

    // -------------------------------------------------------------------------
    // Behaviour: updateIpAddress()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateIpAddress() debe actualizar la dirección IP correctamente")
    void updateIpAddress_shouldReplaceIpAddress() {
        Asset asset = createSampleAsset();
        IpAddress newIp = IpAddress.of("10.10.10.10");

        asset.updateIpAddress(newIp);

        assertEquals(newIp, asset.getIpAddress());
    }

    @Test
    @DisplayName("updateIpAddress() debe lanzar NullPointerException si la nueva IP es null")
    void updateIpAddress_shouldThrowNullPointerException_whenNullIsProvided() {
        Asset asset = createSampleAsset();

        assertThrows(NullPointerException.class, () -> asset.updateIpAddress(null));
    }

    // -------------------------------------------------------------------------
    // equals() — identidad basada en AssetId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Dos assets con el mismo AssetId deben ser iguales aunque tengan distinto nombre y status")
    void equals_shouldBeBasedOnAssetIdOnly() {
        AssetId sharedId = AssetId.generate();

        Asset a1 = Asset.reconstitute(sharedId, "Router A", AssetType.ROUTER,
                IpAddress.of("192.168.0.1"), AssetStatus.ACTIVE, Credentials.of("u1", "p1"));

        Asset a2 = Asset.reconstitute(sharedId, "Router B (renamed)", AssetType.IOT_DEVICE,
                IpAddress.of("192.168.0.2"), AssetStatus.INACTIVE, Credentials.of("u2", "p2"));

        assertEquals(a1, a2);
    }

    // -------------------------------------------------------------------------
    // toString()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toString() debe contener id, name, type, status e ipAddress")
    void toString_shouldContainIdNameTypeStatusAndIpAddress() {
        Asset asset = createSampleAsset();
        String result = asset.toString();

        assertAll("toString debe incluir los campos no sensibles",
                () -> assertTrue(result.contains(asset.getId().toString()),    "debe contener el id"),
                () -> assertTrue(result.contains("Core Router"),               "debe contener el name"),
                () -> assertTrue(result.contains("ROUTER"),                    "debe contener el type"),
                () -> assertTrue(result.contains("ACTIVE"),                    "debe contener el status"),
                () -> assertTrue(result.contains("192.168.1.1"),               "debe contener la ipAddress")
        );
    }

    @Test
    @DisplayName("toString() nunca debe exponer el password de las credentials")
    void toString_shouldNeverExposeCredentialsPassword() {
        Asset asset = createSampleAsset();

        assertFalse(asset.toString().contains("s3cr3t"),
                "toString no debe revelar el password de credentials");
    }
}

