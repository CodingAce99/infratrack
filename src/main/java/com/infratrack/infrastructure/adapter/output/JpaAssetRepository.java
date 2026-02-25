package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;
import com.infratrack.infrastructure.persistence.AssetMapper;
import com.infratrack.infrastructure.persistence.SpringDataAssetRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile({"demo", "prod"})
public class JpaAssetRepository implements AssetRepository {

    private final SpringDataAssetRepository springRepo;

    public JpaAssetRepository(SpringDataAssetRepository springRepo) {
        this.springRepo = Objects.requireNonNull(springRepo, "SpringDataAssetRepository cannot be null");
    }

    @Override
    public Optional<Asset> findById(AssetId id) {
        return springRepo.findById(id.toString()).map(AssetMapper::toDomain);
    }

    @Override
    public List<Asset> findAll() {
        return springRepo.findAll().stream().map(AssetMapper::toDomain).toList();
    }

    @Override
    public void save(Asset asset) {
        springRepo.save(AssetMapper.toJpaEntity(asset));
    }

    @Override
    public void delete(AssetId id) {
        springRepo.deleteById(id.toString());
    }
}

