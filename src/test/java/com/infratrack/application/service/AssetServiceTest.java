package com.infratrack.application.service;

import com.infratrack.application.port.output.AssetRepository;
import com.infratrack.domain.model.Asset;
import com.infratrack.domain.model.AssetId;
import com.infratrack.domain.model.AssetStatus;
import com.infratrack.domain.model.AssetType;
import com.infratrack.domain.model.Credentials;
import com.infratrack.domain.model.IpAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetService — application service")
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(assetRepository);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Asset sampleAsset() {
        return Asset.create(
                "Core Router",
                AssetType.ROUTER,
                IpAddress.of("192.168.1.1"),
                Credentials.of("admin", "s3cr3t")
        );
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("debe lanzar NullPointerException si AssetRepository es null")
        void constructor_shouldThrowNullPointerException_whenRepositoryIsNull() {
            assertThrows(NullPointerException.class, () -> new AssetService(null));
        }
    }

    // -------------------------------------------------------------------------
    // createAsset()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createAsset()")
    class CreateAssetTests {

        @Test
        @DisplayName("debe crear el asset, llamar a save() una vez y devolverlo")
        void createAsset_shouldSaveAndReturnNewAsset() {
            Asset result = assetService.createAsset(
                    "Core Router", AssetType.ROUTER,
                    IpAddress.of("192.168.1.1"), Credentials.of("admin", "s3cr3t")
            );

            assertNotNull(result);
            assertEquals("Core Router", result.getName());
            assertEquals(AssetStatus.ACTIVE, result.getStatus());
            verify(assetRepository, times(1)).save(result);
        }
    }

    // -------------------------------------------------------------------------
    // findAsset()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAsset()")
    class FindAssetTests {

        @Test
        @DisplayName("debe devolver el asset cuando existe en el repositorio")
        void findAsset_shouldReturnAsset_whenItExists() {
            Asset asset = sampleAsset();
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            Asset result = assetService.findAsset(asset.getId());

            assertEquals(asset, result);
            verify(assetRepository, times(1)).findById(asset.getId());
        }

        @Test
        @DisplayName("debe lanzar IllegalArgumentException cuando el asset no existe")
        void findAsset_shouldThrowIllegalArgumentException_whenAssetDoesNotExist() {
            AssetId unknownId = AssetId.generate();
            when(assetRepository.findById(unknownId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> assetService.findAsset(unknownId)
            );

            assertTrue(ex.getMessage().contains(unknownId.toString()),
                    "El mensaje de error debe contener el id buscado");
        }
    }

    // -------------------------------------------------------------------------
    // findAllAssets()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAllAssets()")
    class FindAllAssetsTests {

        @Test
        @DisplayName("debe devolver la lista completa del repositorio")
        void findAllAssets_shouldReturnRepositoryList() {
            List<Asset> assets = List.of(sampleAsset(), sampleAsset());
            when(assetRepository.findAll()).thenReturn(assets);

            List<Asset> result = assetService.findAllAssets();

            assertEquals(assets, result);
            verify(assetRepository, times(1)).findAll();
        }
    }

    // -------------------------------------------------------------------------
    // updateAssetStatus()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateAssetStatus()")
    class UpdateAssetStatusTests {

        @Test
        @DisplayName("debe cambiar el status a ACTIVE y llamar a save()")
        void updateAssetStatus_shouldSetActive() {
            Asset asset = Asset.reconstitute(
                    AssetId.generate(), "Server A", AssetType.SERVER,
                    IpAddress.of("10.0.0.1"), AssetStatus.INACTIVE,
                    Credentials.of("u", "p")
            );
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            Asset result = assetService.updateAssetStatus(asset.getId(), AssetStatus.ACTIVE);

            assertEquals(AssetStatus.ACTIVE, result.getStatus());
            verify(assetRepository, times(1)).save(asset);
        }

        @Test
        @DisplayName("debe cambiar el status a INACTIVE y llamar a save()")
        void updateAssetStatus_shouldSetInactive() {
            Asset asset = sampleAsset(); // comienza en ACTIVE
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            Asset result = assetService.updateAssetStatus(asset.getId(), AssetStatus.INACTIVE);

            assertEquals(AssetStatus.INACTIVE, result.getStatus());
            verify(assetRepository, times(1)).save(asset);
        }

        @Test
        @DisplayName("debe cambiar el status a MAINTENANCE y llamar a save()")
        void updateAssetStatus_shouldSetMaintenance() {
            Asset asset = sampleAsset();
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            Asset result = assetService.updateAssetStatus(asset.getId(), AssetStatus.MAINTENANCE);

            assertEquals(AssetStatus.MAINTENANCE, result.getStatus());
            verify(assetRepository, times(1)).save(asset);
        }
    }

    // -------------------------------------------------------------------------
    // updateAssetCredentials()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateAssetCredentials()")
    class UpdateAssetCredentialsTests {

        @Test
        @DisplayName("debe actualizar las credenciales y llamar a save()")
        void updateAssetCredentials_shouldReplaceCredentialsAndSave() {
            Asset asset = sampleAsset();
            Credentials newCreds = Credentials.of("newuser", "newpass1");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            Asset result = assetService.updateAssetCredentials(asset.getId(), newCreds);

            assertEquals(newCreds, result.getCredentials());
            verify(assetRepository, times(1)).save(asset);
        }
    }

    // -------------------------------------------------------------------------
    // updateAssetIpAddress()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateAssetIpAddress()")
    class UpdateAssetIpAddressTests {

        @Test
        @DisplayName("debe actualizar la IP y llamar a save()")
        void updateAssetIpAddress_shouldReplaceIpAddressAndSave() {
            Asset asset = sampleAsset();
            IpAddress newIp = IpAddress.of("10.20.30.40");
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            Asset result = assetService.updateAssetIpAddress(asset.getId(), newIp);

            assertEquals(newIp, result.getIpAddress());
            verify(assetRepository, times(1)).save(asset);
        }
    }

    // -------------------------------------------------------------------------
    // deleteAsset()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteAsset()")
    class DeleteAssetTests {

        @Test
        @DisplayName("debe llamar a delete() cuando el asset existe")
        void deleteAsset_shouldCallDelete_whenAssetExists() {
            Asset asset = sampleAsset();
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            assetService.deleteAsset(asset.getId());

            verify(assetRepository, times(1)).delete(asset.getId());
        }

        @Test
        @DisplayName("debe lanzar IllegalArgumentException cuando el asset no existe")
        void deleteAsset_shouldThrowIllegalArgumentException_whenAssetDoesNotExist() {
            AssetId unknownId = AssetId.generate();
            when(assetRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> assetService.deleteAsset(unknownId)
            );

            verify(assetRepository, never()).delete(any());
        }
    }
}

