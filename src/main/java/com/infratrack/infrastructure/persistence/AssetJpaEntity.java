package com.infratrack.infrastructure.persistence;


import com.infratrack.infrastructure.security.EncryptedStringConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "assets")
public class AssetJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String username;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String password;

    // --- Constructor vacío requerido por JPA ---

    protected AssetJpaEntity() {
    }

    // --- Constructor completo ---

    public AssetJpaEntity(String id,
                          String name,
                          String type,
                          String ipAddress,
                          String status,
                          String username,
                          String password) {
        this.id        = id;
        this.name      = name;
        this.type      = type;
        this.ipAddress = ipAddress;
        this.status    = status;
        this.username  = username;
        this.password  = password;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

