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
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.createErrorResponse;

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
     * @param envService            the EnvService to be injected
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
    private List<JsonObject> getAllCatalogs(String partnerId, Map<String, String> partnerProperties, DspVersion version) {
        List<? extends DataAsset> allDatasets = dataManagementService.getAll(partnerProperties);
        List<JsonObject> catalogs = new ArrayList<>();

        for (DataAsset dataset : allDatasets) {
            catalogs.add(buildDcatDataset(dataset, partnerId, partnerProperties, version));
        }

        return catalogs;
    }

    /**
     * Builds a DCAT dataset JSON object from a dataset.
     *
     * @param dataAsset the dataset to be converted
     * @return a JSON object representing one DCAT dataset entry
     */
    private JsonObject buildDcatDataset(DataAsset dataAsset, String partnerId, Map<String, String> partnerProperties, DspVersion version) {
        String dcatPrefix = DspVersion.V_08.equals(version) ? "dcat:" : "";
        String dctPrefix = DspVersion.V_08.equals(version) ? "dct:" : "";
        String odrlPrefix = DspVersion.V_08.equals(version) ? "odrl:" : "";
        JsonObjectBuilder distributionBuilder = Json.createObjectBuilder()
                .add("@type", dcatPrefix + "Distribution")
                .add(dctPrefix + "format", "HttpData-PULL")
                .add(dcatPrefix + "accessService", UUID.randomUUID().toString());
        JsonObjectBuilder properties = Json.createObjectBuilder();
        dataAsset.getProperties().forEach(properties::add);
        var policy = Json.createObjectBuilder(policyService.createOfferedPolicy(dataAsset, partnerId, partnerProperties, version)).remove("target");
        JsonObjectBuilder dcatDatasetBuilder = Json.createObjectBuilder()
                .add("@id", String.valueOf(dataAsset.getDspId()))
                .add("@type", dcatPrefix + "Dataset")
                .add(odrlPrefix + "hasPolicy", Json.createArrayBuilder().add(policy))
                .add(dcatPrefix + "distribution", Json.createArrayBuilder().add(distributionBuilder))
                .add("properties", properties);
        return dcatDatasetBuilder.build();
    }

    /**
     * Builds the final JSON object for the catalog response.
     *
     * @param catalogs the list of catalogs as JSON objects
     * @return a JSON object representing the complete catalog response
     */
    private JsonObject buildFinalCatalogResponse(List<JsonObject> catalogs, DspVersion version) {
        String dcatPrefix = DspVersion.V_08.equals(version) ? "dcat:" : "";
        String dspacePrefix = DspVersion.V_08.equals(version) ? "dspace:" : "";
        JsonObjectBuilder body = Json.createObjectBuilder()
                .add("@id", UUID.randomUUID().toString())
                .add("@type", dcatPrefix + "Catalog");

        JsonArrayBuilder datasetArrayBuilder = Json.createArrayBuilder();
        catalogs.forEach(datasetArrayBuilder::add);
        body.add(dcatPrefix + "dataset", datasetArrayBuilder);
        body.add(dcatPrefix + "service", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("@id", UUID.randomUUID().toString())
                        .add("@type", dcatPrefix + "DataService")
                        .add(dcatPrefix + "endpointDescription", dspacePrefix + "connector")
                        .add(dcatPrefix + "endpointURL", envService.getOwnDspUrl())))
                .add(dspacePrefix + "participantId", envService.getBackendId())
                .add("@context", JsonUtils.getContextForDspVersion(version));
        return body.build();
    }

    /**
     * Build a catalog response for the given partner.
     *
     * @param partnerId the partner
     * @return the catalog
     */
    public JsonObject getFullCatalog(String partnerId, Map<String, String> partnerProperties, DspVersion version) {
        return buildFinalCatalogResponse(getAllCatalogs(partnerId, partnerProperties, version), version);
    }

    public String getDataset(String partnerId, Map<String, String> partnerProperties, String id, DspVersion version) {
        DataAsset asset = dataManagementService.getByIdForProperties(id, partnerProperties);
        if (asset == null) {
            return new String(createErrorResponse("unknown", "unknown",
                    "CatalogError", List.of("Bad Request"), version));
        }
        var datasetPolicy = Json.createObjectBuilder(
                policyService.createOfferedPolicy(asset,partnerId, partnerProperties, version))
                .remove("target").build();

        JsonObjectBuilder datasetBuilder = Json.createObjectBuilder();
        datasetBuilder.add("@context",
                JsonUtils.getContextForDspVersion(version));
        datasetBuilder.add("@id", asset.getDspId());
        datasetBuilder.add("@type", "Dataset");
        datasetBuilder.add("hasPolicy",
                Json.createArrayBuilder().add(datasetPolicy));
        datasetBuilder.add("distribution",
                Json.createArrayBuilder().add(
                        Json.createObjectBuilder()
                                .add("@type", "Distribution")
                                .add("format", "HttpData-PULL")
                                .add("accessService", Json.createObjectBuilder()
                                        .add("@id", UUID.randomUUID().toString())
                                        .add("@type", "DataService")
                                        .add("endpointURL", envService.getOwnDspUrl() + version.PATH_SUFFIX)
                                        .build())
                ));
        return datasetBuilder.build().toString();
    }
}
