package com.infratrack.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAssetRepository extends JpaRepository<AssetJpaEntity, String> {
}

