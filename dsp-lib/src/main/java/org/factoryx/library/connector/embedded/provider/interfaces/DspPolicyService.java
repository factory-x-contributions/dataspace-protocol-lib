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
import jakarta.json.*;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;

import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.*;

/**
 * This abstract class provides the interface for the DspPolicyService.
 * <p>
 * The customization for the requirements of a concrete dataspace can be done
 * by overriding the getPermission, getProhibition and getObligation methods.
 *
 */
@Slf4j
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
    public final JsonObject createOfferedPolicy(String assetId, String partnerId, DspVersion version) {
        String prefix = DspVersion.V_08.equals(version) ? "odrl:" : "";
        var builder = Json.createObjectBuilder()
                .add("@id", UUID.randomUUID().toString())
                .add("@type", prefix + "Offer")
                .add(prefix + "assigner", envService.getBackendId())
                .add(prefix + "assignee", partnerId)
                .add(prefix + "target", Json.createObjectBuilder().add(ID, assetId).build());
        var permission = getPermission(assetId, partnerId, version);
        if (permission != null && !isEmpty(permission)) {
            builder.add(prefix + "permission", permission);
        }
        var prohibition = getProhibition(assetId, partnerId, version);
        if (prohibition != null && !isEmpty(prohibition)) {
            builder.add(prefix + "prohibition", prohibition);
        }
        var obligation = getObligation(assetId, partnerId, version);
        if (obligation != null && !isEmpty(obligation)) {
            builder.add(prefix + "obligation", obligation);
        }
        return builder.build();

    }

    public static boolean isEmpty(JsonValue value) {
        if (value instanceof JsonArray array) {
            return array.isEmpty();
        }
        if (value instanceof JsonObject object) {
            return object.isEmpty();
        }
        if (value instanceof JsonString string) {
            return string.getString().isEmpty();
        }
        throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
    }

    public JsonValue getPermission(String assetId, String partnerId, DspVersion version) {
        return JsonValue.EMPTY_JSON_ARRAY;
    }

    public JsonValue getProhibition(String assetId, String partnerId, DspVersion version) {
        return JsonValue.EMPTY_JSON_ARRAY;
    }

    public JsonValue getObligation(String assetId, String partnerId, DspVersion version) {
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
    public boolean validateOffer(JsonObject offer, String targetAssetId, String partnerId, DspVersion version) {
        try {
            JsonObject offerObject = sanitizeOffer(offer);
            JsonObject expectedObject = sanitizeOffer(createExpandedPolicy(targetAssetId, partnerId, version));
            return offerObject.equals(expectedObject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // helper methods:

    /**
     * When comparing an offer to an expected counterpart, the "@id" values are not required to match.
     * Therefore, we remove it before comparing. Also, we will remove permission, obligation and prohibition
     * arrays, if empty.
     *
     * @param offer an offer received from a negotiation partner
     * @return the same object, but without an "@id" field and without empty arrays
     */
    protected JsonObject sanitizeOffer(JsonObject offer) {
        var offerBuilder = Json.createObjectBuilder(offer);
        offerBuilder.remove(ID);
        var permission = offer.getJsonArray(ODRL_NAMESPACE + "permission");
        if (permission != null && permission.isEmpty()) {
            offerBuilder.remove(ODRL_NAMESPACE + "permission");
        }
        var prohibition = offer.getJsonArray(ODRL_NAMESPACE + "prohibition");
        if (prohibition != null && prohibition.isEmpty()) {
            offerBuilder.remove(ODRL_NAMESPACE + "prohibition");
        }
        var obligation = offer.getJsonArray(ODRL_NAMESPACE + "obligation");
        if (obligation != null && obligation.isEmpty()) {
            offerBuilder.remove(ODRL_NAMESPACE + "obligation");
        }
        for (JsonValue value : offer.getJsonArray(ODRL_NAMESPACE + "assignee")) {
            if (value instanceof JsonObject object) {
                var assignee = object.getJsonString("@id");
                if (assignee != null) {
                    offerBuilder.remove(ODRL_NAMESPACE + "assignee");
                    offerBuilder.add(ODRL_NAMESPACE + "assignee", Json.createArrayBuilder().add(Json.createObjectBuilder().add("@value", assignee)));
                }
            }
        }
        for (JsonValue value : offer.getJsonArray(ODRL_NAMESPACE + "assigner")) {
            if (value instanceof JsonObject object) {
                var assigner = object.getJsonString("@id");
                if (assigner != null) {
                    offerBuilder.remove(ODRL_NAMESPACE + "assigner");
                    offerBuilder.add(ODRL_NAMESPACE + "assigner", Json.createArrayBuilder().add(Json.createObjectBuilder().add("@value", assigner)));
                }
            }
        }
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
    protected JsonObject createExpandedPolicy(String targetAssetId, String partnerId, DspVersion version) {
        try {
            var wrapper = Json.createObjectBuilder();
            var odrlContext = Json.createObjectBuilder()
                    .add("@vocab", ODRL_NAMESPACE)
                    .add("dspace", DSPACE_NAMESPACE)
                    .add("odrl", ODRL_NAMESPACE);
            wrapper.add("@context", odrlContext.build());
            wrapper.add("dspace:Offer", createOfferedPolicy(targetAssetId, partnerId, version));
            return JsonLd.expand(JsonDocument.of(wrapper.build()))
                    .get()
                    .getJsonObject(0)
                    .getJsonArray(DSPACE_NAMESPACE + "Offer")
                    .getJsonObject(0);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
