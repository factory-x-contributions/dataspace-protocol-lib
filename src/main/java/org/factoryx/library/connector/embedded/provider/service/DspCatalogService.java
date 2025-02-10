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

package org.factoryx.library.connector.embedded.provider.service;

import jakarta.json.*;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAsset;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAssetManagementService;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
/**
 * This services creates a response body for a DSP catalog request
 *
 * @author tobias.urbanek
 */
public class DspCatalogService {

    private final DataAssetManagementService dataManagementService;

    private final EnvService envService;

    private final DspPolicyService policyService;

    /**
     * Constructor for injecting the DataManagementService and EnvService.
     *
     * @param dataManagementService the DataManagementService to be injected
     * @param envService the EnvService to be injected
     */
    @Autowired
    public DspCatalogService(DataAssetManagementService dataManagementService, EnvService envService, DspPolicyService policyService) {
        this.dataManagementService = dataManagementService;
        this.envService = envService;
        this.policyService = policyService;
    }

    /**
     * Retrieves all catalogs by converting datasets to JSON objects.
     *
     * @return a list of JSON objects representing the DCAT datasets
     */
    private List<JsonObject> getAllCatalogs(String partnerId){
        List<? extends DataAsset> allDatasets = dataManagementService.getAll();
        List<JsonObject> catalogs = new ArrayList<>();

        for (DataAsset dataset : allDatasets) {
            catalogs.add(buildDcatDataset(dataset, partnerId));
        }

        return catalogs;
    }

    /**
     * Builds a DCAT dataset JSON object from a dataset.
     *
     * @param dataset the dataset to be converted
     * @return a JSON object representing one DCAT dataset entry
     */
    private JsonObject buildDcatDataset(DataAsset dataset, String partnerId) {
        JsonObjectBuilder distributionBuilder = Json.createObjectBuilder()
                .add("@type", "dcat:Distribution")
                .add("dct:format", Json.createObjectBuilder()
                        .add("@id", "HttpData-PULL"));
        JsonObjectBuilder properties = Json.createObjectBuilder();
        dataset.getProperties().forEach(properties::add);
        JsonObjectBuilder dcatDatasetBuilder = Json.createObjectBuilder()
                .add("@id", String.valueOf(dataset.getId()))
                .add("@type", "dcat:Dataset")
                .add("odrl:hasPolicy", policyService.createOfferedPolicy(dataset.getId().toString(), partnerId))
                .add("dcat:distribution", distributionBuilder)
                .add("properties", properties);

        return dcatDatasetBuilder.build();
    }

    /**
     * Builds the final JSON object for the catalog response.
     *
     * @param catalogs the list of catalogs as JSON objects
     * @return a JSON object representing the complete catalog response
     */
    private JsonObject buildFinalCatalogResponse(List<JsonObject> catalogs) {
        JsonObjectBuilder body = Json.createObjectBuilder()
                .add("@id", UUID.randomUUID().toString())
                .add("@type", "dcat:Catalog");

        if (catalogs.size() != 1) {
            JsonArrayBuilder datasetArrayBuilder = Json.createArrayBuilder();
            catalogs.forEach(datasetArrayBuilder::add);
            body.add("dcat:dataset", datasetArrayBuilder);
        } else {
            body.add("dcat:dataset", catalogs.getFirst());
        }
        body.add("dcat:service", Json.createObjectBuilder()
                        .add("@id", UUID.randomUUID().toString())
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointDescription", "dspace:connector")
                        .add("dcat:endpointUrl", envService.getOwnDspUrl()))
                .add("dspace:participantId", envService.getBackendId())
                .add("@context", JsonUtils.FULL_CONTEXT);
        return body.build();
    }

    /**
     * Build a catalog response for the given partner.
     *
     * @param partnerId the partner
     * @return the catalog
     */
    public JsonObject getFullCatalog(String partnerId) {
        return buildFinalCatalogResponse(getAllCatalogs(partnerId));
    }
}
