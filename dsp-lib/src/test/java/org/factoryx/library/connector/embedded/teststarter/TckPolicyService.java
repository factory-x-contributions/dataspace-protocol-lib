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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "org.factoryx.library.policyservice", havingValue = "tck")
public class TckPolicyService extends DspPolicyService {

    public TckPolicyService(EnvService envService) {
        super(envService);
    }

    @Override
    public JsonValue getPermission(String assetId, String partnerId, DspVersion version) {
        return switch (assetId) {
            case SampleDataAsset.CATALOG_ASSET_ID -> getCatalogTestContent();
            case SampleDataAsset.NEGOTIATION_ASSET_ID -> getNegotiationTestContent(assetId);
            default -> super.getPermission(assetId, partnerId, version);
        };
    }

    private JsonArray getNegotiationTestContent(String assetId) {
        return Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("action", "use").build())
                .build();
    }

    private JsonArray getCatalogTestContent() {
        return Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("action", "use")
                        .add("constraint",
                                Json.createArrayBuilder().add(
                                                Json.createObjectBuilder().add("leftOperand", "spatial")
                                                        .add("rightOperand", "_:EU")
                                                        .add("operator", "eq")
                                                        .build())
                                        .build()
                        ).build()
                ).build();
    }

    @Override
    public JsonValue getProhibition(String assetId, String partnerId, DspVersion version) {
        return switch (assetId) {
            case SampleDataAsset.CATALOG_ASSET_ID -> getCatalogTestContent();
            default -> super.getPermission(assetId, partnerId, version);
        };
    }

    @Override
    public JsonValue getObligation(String assetId, String partnerId, DspVersion version) {
        return switch (assetId) {
            case SampleDataAsset.CATALOG_ASSET_ID -> getCatalogTestContent();
            default -> super.getPermission(assetId, partnerId, version);
        };
    }

    @Override
    public boolean validateOffer(JsonObject offer, String targetAssetId, String partnerId, DspVersion version) {
        return true;
    }
}
