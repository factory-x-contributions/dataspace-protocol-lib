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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.*;

public class V_2025_1_Deserializer {

    public final static String DSP_2025_NAMESPACE = "https://w3id.org/dspace/2025/1/";
    public final static String EXPECTED_CONTEXT = "\"" + DSP_2025_NAMESPACE + "context.jsonld\"";

    private static final Logger log = LoggerFactory.getLogger(V_2025_1_Deserializer.class);


    public static TransferStartMessage deserializeTransferStartMessage(String rawJson) {
        try {
            return new TransferStartMessageImpl(rawJson);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Getter
    static class TransferStartMessageImpl implements TransferStartMessage {
        @NonNull
        private final String consumerPid;
        @NonNull
        private final UUID providerPid;
        TransferStartMessageImpl(String rawJson) {
            JsonObject node = parse(rawJson);
            this.consumerPid = node.getString("consumerPid");
            this.providerPid = UUID.fromString(node.getString("providerPid"));
        }
    }

    public static TransferSuspensionMessage deserializeTransferSuspensionMessage(String rawJson) {
        try {
            return new TransferSuspensionMessageImpl(rawJson);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Getter
    static class TransferSuspensionMessageImpl implements TransferSuspensionMessage {
        @NonNull
        private final String consumerPid;
        @NonNull
        private final UUID providerPid;
        TransferSuspensionMessageImpl(String rawJson) {
            JsonObject node = parse(rawJson);
            this.consumerPid = node.getString("consumerPid");
            this.providerPid = UUID.fromString(node.getString("providerPid"));
        }
    }

    public static TransferTerminationMessage deserializeTransferTerminationMessage(String rawJson) {
        try {
            return new TransferTerminationMessageImpl(rawJson);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Getter
    static class TransferTerminationMessageImpl implements TransferTerminationMessage {
        @NonNull
        private final String consumerPid;
        @NonNull
        private final UUID providerPid;
        TransferTerminationMessageImpl(String rawJson) {
            JsonObject node = parse(rawJson);
            this.consumerPid = node.getString("consumerPid");
            this.providerPid = UUID.fromString(node.getString("providerPid"));
        }
    }

    public static TransferCompletionMessage deserializeTransferCompletionMessage(String rawJson) {
        try {
            return new TransferCompletionMessageImpl(rawJson);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Getter
    static class TransferCompletionMessageImpl implements TransferCompletionMessage {
        @NonNull
        private final String consumerPid;
        @NonNull
        private final UUID providerPid;

        TransferCompletionMessageImpl(String rawJson) {
            JsonObject node = parse(rawJson);
            this.consumerPid = node.getString("consumerPid");
            this.providerPid = UUID.fromString(node.getString("providerPid"));
        }
    }


    public static TransferRequestMessage deserializeTransferRequestMessage(String rawJson) {
        try {
            return new TransferRequestMessageImpl(rawJson);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Getter
    @ToString
    static class TransferRequestMessageImpl implements TransferRequestMessage {
        private final String consumerPid;
        private final UUID agreementId;
        private final String partnerDspUrl;
        private final String format;

        TransferRequestMessageImpl(String rawJson) {
            JsonObject node = parse(rawJson);
            this.consumerPid = node.getString("consumerPid");
            this.agreementId = UUID.fromString(node.getString("agreementId"));
            this.partnerDspUrl = node.getString("callbackAddress");
            this.format = node.getString("format");
        }

    }


    public static NegotiationTerminationMessage deserializeContractTerminationMessage(String rawJson) {
        try {
            return new NegotiationTerminationImpl(rawJson);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Getter
    private static class NegotiationTerminationImpl implements NegotiationTerminationMessage {
        @NonNull
        private UUID providerPid;
        @NonNull
        private String consumerPid;

        NegotiationTerminationImpl(String rawJson) {
            JsonObject node = parse(rawJson);
            log.info("rawJson at constructor \n{}", prettyPrint(node));

            this.consumerPid = node.getString("consumerPid");
            this.providerPid = UUID.fromString(node.getString("providerPid"));
        }

    }

    public static CatalogRequestMessage deserializeCatalogRequest(String json) {
        try {
            JsonObject node = parse(json);
            if ("CatalogRequestMessage".equals(node.getString("@type"))) {
                return new V_08_Deserializer.CatalogRequestImpl();
            }
            log.warn("Invalid catalog request json: \n{}", prettyPrint(node));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static ContractRequestMessage deserializeContractRequest(String json) {
        try {
            return new ContractRequestImpl(json);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Getter
    @ToString
    static class ContractRequestImpl implements ContractRequestMessage {

        @NonNull
        private final String consumerPid;
        @NonNull
        private final String targetAssetId;
        @NonNull
        private final String partnerDspUrl;

        private final JsonObject offer;

        ContractRequestImpl(String rawJson) {
            JsonObject node = parse(rawJson);
            String messageType = node.getString("@type");
            if (!messageType.equals("ContractRequestMessage")) {
                log.error("Invalid message type: {}", messageType);
                throw new IllegalArgumentException("Invalid message type: " + messageType);
            }
            JsonArray context = node.getJsonArray("@context");
            if (context.stream().noneMatch(x -> EXPECTED_CONTEXT.equals(x.toString()))) {
                log.error("Invalid Context");
                throw new IllegalArgumentException("Invalid context: " + context);
            }
            this.consumerPid = node.getString("consumerPid");
            this.targetAssetId = node.getJsonObject("offer").getString("target");
            this.partnerDspUrl = node.getString("callbackAddress");
            JsonObject temp = null;
            try {
                var expandedNode = parseAndExpand(rawJson);
                temp = expandedNode.getJsonArray(DSP_2025_NAMESPACE + "offer").getJsonObject(0);
            } catch (Exception e) {
            }
            this.offer = temp;

        }
    }

    public static ContractVerificationMessage deserializeContractVerification(String rawJson) {
        try {
            return new ContractVerificationImpl(rawJson);
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
            JsonObject node = parse(rawJson);
            String messageType = node.getString("@type");
            if (!messageType.equals("ContractAgreementVerificationMessage")) {
                throw new IllegalArgumentException("Invalid message type: " + messageType);
            }
            JsonArray context = node.getJsonArray("@context");
            if (context.size() != 1 && !context.getString(0).equals("https://w3id.org/dspace/2025/1/context.jsonld")) {
                throw new IllegalArgumentException("Invalid context: " + context);
            }
            this.consumerPid = node.getString("consumerPid");
            this.providerPid = node.getString("providerPid");
        }
    }
}
