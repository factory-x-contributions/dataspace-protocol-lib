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

package org.factoryx.library.connector.embedded.provider.service.deserializers;

import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.*;


public class V_08_Deserializer {

    static final private Logger log = LoggerFactory.getLogger(V_08_Deserializer.class);


    public static TransferStartMessage deserializeTransferStartMessage(String rawJson) {
        return V_2025_1_Deserializer.deserializeTransferStartMessage(rawJson);
    }

    public static TransferSuspensionMessage deserializeTransferSuspensionMessage(String rawJson) {
        return V_2025_1_Deserializer.deserializeTransferSuspensionMessage(rawJson);
    }

    public static TransferTerminationMessage deserializeTransferTerminationMessage(String rawJson) {
        return V_2025_1_Deserializer.deserializeTransferTerminationMessage(rawJson);
    }

    public static TransferCompletionMessage deserializeTransferCompletionMessage(String rawJson) {
        return V_2025_1_Deserializer.deserializeTransferCompletionMessage(rawJson);
    }

    public static TransferRequestMessage deserializeTransferRequestMessage(String rawJson) {
        try {
            return new TransferRequestMessageImpl(rawJson);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Getter
    @ToString
    static class TransferRequestMessageImpl implements TransferRequestMessage {
        private final String consumerPid;
        private final UUID agreementId;
        private final String format;
        private final String partnerDspUrl;

        TransferRequestMessageImpl(String rawJson) {
            JsonObject node = parse(rawJson);
            this.consumerPid = node.getString("consumerPid");
            this.agreementId = UUID.fromString(node.getString("agreementId"));
            this.partnerDspUrl = node.getString("callbackAddress");
            this.format = node.getString("format");
        }
    }

    public static NegotiationTerminationMessage deserializeContractTerminationMessage(String rawJson) {
        return V_08_Deserializer.deserializeContractTerminationMessage(rawJson);
    }


    public static CatalogRequestMessage deserializeCatalogRequest(String json) {
        try {
            JsonObject node = parse(json);
            if ("dspace:CatalogRequestMessage".equals(node.getString("@type"))) {
                return new CatalogRequestImpl();
            }
            log.warn("Invalid catalog request json: \n{}", prettyPrint(node));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * CATALOG SECTION
     */


    public static ContractRequestMessage deserializeContractRequest(String json) {
        try {
            return new ContractRequestImpl(json);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    record CatalogRequestImpl() implements CatalogRequestMessage {
    }

    /**
     * CONTRACT NEGOTIATION SECTION
     */

    @Getter
    @ToString
    private static class ContractRequestImpl implements ContractRequestMessage {

        @NonNull
        private final String consumerPid;
        @NonNull
        private final String targetAssetId;
        @NonNull
        private final String partnerDspUrl;
        @NonNull
        private final JsonObject offer;

        ContractRequestImpl(String rawJson) {
            JsonObject node = parseAndExpand(rawJson);
            String messageType = node.getJsonArray("@type").getString(0);
            if (!messageType.equals(DSPACE_NAMESPACE + "ContractRequestMessage")) {
                throw new IllegalArgumentException("Invalid message type: " + messageType);
            }
            this.consumerPid = node.getJsonArray(DSPACE_NAMESPACE + "consumerPid").getJsonObject(0).getString("@value");
            this.partnerDspUrl = node.getJsonArray(DSPACE_NAMESPACE + "callbackAddress").getJsonObject(0).getString("@value");
            this.offer = node.getJsonArray(DSPACE_NAMESPACE + "offer").getJsonObject(0);
            this.targetAssetId = offer.getJsonArray(ODRL_NAMESPACE + "target").getJsonObject(0).getString("@id");
        }
    }


    public static ContractVerificationMessage deserializeContractVerification(String json) {
        try {
            return new ContractVerificationImpl(json);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Getter
    @ToString
    private static class ContractVerificationImpl implements ContractVerificationMessage {
        @NonNull
        private final String consumerPid;
        @NonNull
        private final String providerPid;

        ContractVerificationImpl(String rawJson) {
            JsonObject node = parseAndExpand(rawJson);
            String messageType = node.getJsonArray("@type").getString(0);
            if (!messageType.equals(DSPACE_NAMESPACE + "ContractAgreementVerificationMessage")) {
                throw new IllegalArgumentException("Invalid message type: " + messageType);
            }
            consumerPid = node.getJsonArray(DSPACE_NAMESPACE + "consumerPid").getJsonObject(0).getString("@value");
            providerPid = node.getJsonArray(DSPACE_NAMESPACE + "providerPid").getJsonObject(0).getString("@value");
        }
    }


}
