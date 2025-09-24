/*
 * Copyright (c) 2025. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

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
    public DataAsset getById(String id) {
        return dataAssets.stream().filter(dataAsset -> dataAsset.getDspId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public DataAsset getByIdForProperties(String id, Map<String, String> partnerProperties) {
        return getById(id);
    }

    @Override
    public List<DataAsset> getAll(Map<String, String> partnerProperties) {
        return new ArrayList<>(dataAssets);
    }


    public void addTckDataAsset(String id) {
        dataAssets.add(new SampleDataAsset(UUID.fromString(id)));
    }

}
