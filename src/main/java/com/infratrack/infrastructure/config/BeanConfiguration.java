package com.infratrack.infrastructure.config;

import com.infratrack.application.port.input.ManageAssetUseCase;
import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.application.service.AssetService;
import com.infratrack.infrastructure.adapter.output.InMemoryAssetRepository;
import com.infratrack.infrastructure.adapter.output.JpaAssetRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class BeanConfiguration {

    @Bean
    @Profile("dev")
    public AssetRepository inMemoryAssetRepository() {
        return new InMemoryAssetRepository();
    }

    @Bean
    @Profile({"demo", "prod"})
    public AssetRepository jpaAssetRepository(JpaAssetRepository jpaAssetRepository) {
        return jpaAssetRepository;
    }

    @Bean
    public ManageAssetUseCase manageAssetUseCase(AssetRepository assetRepository) {
        return new AssetService(assetRepository);
    }
}

