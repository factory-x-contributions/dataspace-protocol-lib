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

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.DSPACE_NAMESPACE;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.ODRL_NAMESPACE;

/**
 * This abstract class provides the interface for the DspPolicyService.
 * <p>
 * The customization for the requirements of a concrete dataspace can be done
 * by overriding the getPermission, getProhibition and getObligation methods.
 *
 */
public abstract class DspPolicyService {

    protected final EnvService envService;

    protected static final String ID = "@id";

    protected DspPolicyService(EnvService envService) {
        this.envService = envService;
    }

    // public interface methods:

    /**
     * This method returns an odrl:Offer typed object with the following structure:
     * <p>
     * {
     * <p>
     *     "@type": "odrl:Offer",
     * <p>
     *     "odrl:permission": [ ... ],
     * <p>
     *     "odrl:prohibition": [ ... ],
     * <p>
     *     "odrl:obligation": [ ... ],
     * <p>
     *     "odrl:assignee": "<string>",
     * <p>
     *     "odrl:assigner": "<string>",
     * <p>
     *     "odrl:target":
     *     <p>
     *                      {
     *                      <p>
     *                          "@id" : "<string"
     *                          <p>
     *                      }
     * <p>
     * }
     * <p>
     *
     * Note that you can and should provide customized implementations of the getPermission, getProhibition
     * and getObligation methods as required by the framework agreements of the dataspace you want to participate in.
     *
     * @param assetId the id of the asset in question
     * @param partnerId the id of the partner who is interested in the given asset
     * @return the JSON object
     */
    public final JsonObject createOfferedPolicy(String assetId, String partnerId) {
        return Json.createObjectBuilder()
                .add("@type", "odrl:Offer")
                .add("odrl:permission", getPermission(assetId, partnerId))
                .add("odrl:prohibition", getProhibition(assetId, partnerId))
                .add("odrl:obligation", getObligation(assetId, partnerId))
                .add("odrl:assigner", envService.getBackendId())
                .add("odrl:assignee", partnerId)
                .add("odrl:target", Json.createObjectBuilder().add(ID, assetId).build())
                .build();
    }

    public JsonValue getPermission(String assetId, String partnerId) {
        return JsonValue.EMPTY_JSON_ARRAY;
    }

    public JsonValue getProhibition(String assetId, String partnerId) {
        return JsonValue.EMPTY_JSON_ARRAY;
    }

    public JsonValue getObligation(String assetId, String partnerId) {
        return JsonValue.EMPTY_JSON_ARRAY;
    }

    /**
     * This method expects an offer object from a new negotiation request and checks if
     * the given object matches the expected content.
     *
     *
     * @param offer the offer object
     * @param targetAssetId the target asset id
     * @param partnerId the id of the negotiation partner
     * @return true if the offer is valid.
     */
    public final boolean validateOffer(JsonObject offer, String targetAssetId, String partnerId) {
        try {
            JsonObject offerObject = removeId(offer);
            JsonObject expectedObject = createExpandedPolicy(targetAssetId, partnerId);
            return offerObject.equals(expectedObject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // helper methods:

    /**
     * When comparing an offer to an expected counterpart, the "@id" values are not required to match.
     * Therefore, we remove it before comparing.
     *
     * @param offer an offer received from a negotiation partner
     * @return the same object, but without an "@id" field
     */
    private JsonObject removeId(JsonObject offer) {
        var offerBuilder = Json.createObjectBuilder(offer);
        offerBuilder.remove(ID);
        return offerBuilder.build();
    }

    /**
     * Since the negotiation request from a partner is expanded, we need to expand
     * our own comparison object as well.
     *
     * @param targetAssetId the id of the asset in question
     * @param partnerId the id of the negotiation partner
     * @return the expanded object that can be compared with the partner's offer
     */
    private JsonObject createExpandedPolicy(String targetAssetId, String partnerId) {
        try {
            var wrapper = Json.createObjectBuilder();
            var odrlContext = Json.createObjectBuilder()
                    .add("@vocab", ODRL_NAMESPACE)
                    .add("dspace", DSPACE_NAMESPACE)
                    .add("odrl", ODRL_NAMESPACE);
            wrapper.add("@context", odrlContext.build());
            wrapper.add("dspace:Offer", createOfferedPolicy(targetAssetId, partnerId));
            return JsonLd.expand(JsonDocument.of(wrapper.build()))
                    .get()
                    .getJsonObject(0)
                    .getJsonArray(DSPACE_NAMESPACE + "Offer")
                    .getJsonObject(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
