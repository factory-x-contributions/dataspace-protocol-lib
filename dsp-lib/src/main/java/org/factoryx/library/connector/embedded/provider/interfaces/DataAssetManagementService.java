/*
 * Copyright (c) 2024. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.factoryx.library.connector.embedded.provider.interfaces;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

/**
 * A class implementing this interface is expected to provide read-access to
 * the library on single DataAssets and a complete list of all available DataAssets.
 *
 * @author eschrewe
 *
 */
public interface DataAssetManagementService {

    /**
     * Retrieve a single DataAsset entity with the given id.
     * Attention: This method should only be used by the DataAccess controller since it is skipping any DCP-related
     * credential information because it assumes that this has already been checked during the DSP
     * negotiation and transfer flow.
     *
     * @param id The id that specifies a DataAsset
     * @return the DataAsset entity
     */
    DataAsset getById(String id);


    /**
     * Retrieve a single DataAsset entity with the given id, if the partnerProperties are sufficient.
     *
     * The use-case specific implementation of this interface can and should create mechanisms to determine
     * which asset is visible for which kind of partner properties.
     *
     * @param id The UUID that specifies a DataAsset
     * @param partnerProperties The properties of the partner that were found by the DspValidationService in his
     *                          verifiable credentials
     * @return the DataAsset entity
     */
    DataAsset getByIdForProperties(String id, Map<String, String> partnerProperties);

    /**
     * Retrieve a list of all available DataAssets (except for those where the partnerProperties are insufficient).
     *
     * The use-case specific implementation of this interface can and should create mechanisms to determine
     * which asset is visible for which kind of partner properties.
     *
     * @param partnerProperties The properties of the partner that were found by the DspValidationService in his
     *                          verifiable credentials
     * @return the list of DataAssets
     */
    List<DataAsset> getAll(Map<String, String> partnerProperties);

    default ResponseEntity<byte[]> forwardToApiAsset(String apiAssetId, HttpMethod method, byte[] requestBody,
                                                     HttpHeaders headers, String path, MultiValueMap<String, String> incomingQueryParams) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
