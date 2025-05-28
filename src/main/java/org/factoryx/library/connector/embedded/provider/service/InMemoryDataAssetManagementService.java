package org.factoryx.library.connector.embedded.provider.service;

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAsset;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAssetManagementService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
/**
 * Mock implementation of {@link DataAssetManagementService} that returns no data.
 * This service is used as a placeholder or fallback and does not persist or manage any real data assets.
 */
public class InMemoryDataAssetManagementService implements DataAssetManagementService {

    @Override
    public DataAsset getById(UUID id) {
        log.info("Mock getById: {}", id);
        return null;
    }

    @Override
    public List<? extends DataAsset> getAll() {
        log.info("Mock getAll");
        return Collections.emptyList();
    }
}
