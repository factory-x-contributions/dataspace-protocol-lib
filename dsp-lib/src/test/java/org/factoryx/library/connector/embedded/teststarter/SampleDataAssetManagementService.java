package org.factoryx.library.connector.embedded.teststarter;

import org.factoryx.library.connector.embedded.provider.interfaces.DataAsset;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAssetManagementService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SampleDataAssetManagementService implements DataAssetManagementService {

    private List<DataAsset> dataAssets = new ArrayList<>();


    @Override
    public DataAsset getById(UUID id) {
        return dataAssets.stream().filter(dataAsset -> dataAsset.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public DataAsset getByIdForProperties(UUID id, Map<String, String> partnerProperties) {
        return getById(id);
    }

    @Override
    public List<? extends DataAsset> getAll(Map<String, String> partnerProperties) {
        return new ArrayList<>(dataAssets);
    }


    public void addTckDataAsset() {
        dataAssets.add(new SampleDataAsset(UUID.fromString("207ed5a4-2eae-47af-bcb1-9202280d2700")));
    }


    static List<DataAsset> getDataAssets(int count) {
        List<DataAsset> dataAssets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            dataAssets.add(new SampleDataAsset());
        }
        return dataAssets;
    }
}
