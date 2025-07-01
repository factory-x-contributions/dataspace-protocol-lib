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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAssetManagementService;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.ResponseRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.SendContractAgreedTask;
import org.factoryx.library.connector.embedded.provider.service.helpers.SendContractFinalizedTask;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.*;


/**
 * This service contains the logic for handling incoming negotiation requests in the DSP context
 *
 * @author eschrewe
 *
 */
@Service
@Slf4j
public class DspNegotiationService {

    private final NegotiationRecordService negotiationRecordService;

    private final DataAssetManagementService dataManagementService;

    private final ExecutorService executorService;

    private final RestClient restClient;

    private final EnvService envService;

    private final DspTokenProviderService dspTokenProviderService;

    private final DspPolicyService policyService;


    public DspNegotiationService(NegotiationRecordService negotiationRecordService, DataAssetManagementService dataManagementService,
                                 ExecutorService executorService, RestClient restClient, EnvService envService,
                                 DspTokenProviderService dspTokenProviderService, DspPolicyService policyService) {
        this.negotiationRecordService = negotiationRecordService;
        this.dataManagementService = dataManagementService;
        this.executorService = executorService;
        this.restClient = restClient;
        this.envService = envService;
        this.dspTokenProviderService = dspTokenProviderService;
        this.policyService = policyService;
    }

    /**
     * This method handles new incoming contract negotiation requests from the /dsp/negotiations/request endpoint.
     *
     * @param requestBody - the request body of the incoming message
     * @param partnerId - the id of the requesting party as retrieved from the HTTP auth token
     * @return - a response ACK-body and code 201, if successful
     */
    public ResponseRecord handleNewNegotiation(String requestBody, String partnerId, Map<String, String> partnerProperties) {
        JsonObject requestJson = parseAndExpand(requestBody);
        String consumerPid, messageType, partnerDspUrl, targetAssetId;
        JsonObject offer;
        int offerSize;
        try {
            consumerPid = requestJson.getJsonArray(DSPACE_NAMESPACE + "consumerPid").getJsonObject(0).getString("@value");
            messageType = requestJson.getJsonArray("@type").getString(0);
            partnerDspUrl = requestJson.getJsonArray(DSPACE_NAMESPACE + "callbackAddress").getJsonObject(0).getString("@value");
            JsonArray offers = requestJson.getJsonArray(DSPACE_NAMESPACE + "offer");
            offerSize = offers.size();
            offer = offers.getJsonObject(0);
            targetAssetId = offer.getJsonArray(ODRL_NAMESPACE + "target").getJsonObject(0).getString("@id");
        } catch (Exception e) {
            return new ResponseRecord("Invalid request.".getBytes(StandardCharsets.UTF_8), 400);
        }
        NegotiationRecord newRecord = negotiationRecordService.createNegotiationRecord(consumerPid, partnerId, partnerDspUrl,
                targetAssetId);

        if (!messageType.equals(DSPACE_NAMESPACE + "ContractRequestMessage")) {
            log.warn("Wrong message type: {} at /negotiations/request", messageType);
            newRecord = negotiationRecordService.updateNegotiationRecordToState(newRecord.getOwnPid(), NegotiationState.TERMINATED);
            return new ResponseRecord(createErrorResponse(newRecord.getOwnPid().toString(), consumerPid, "ContractNegotiationError",
                    List.of("Wrong message type: " + messageType)), 400);
        }

        if (offerSize != 1 || !policyService.validateOffer(offer, newRecord.getTargetAssetId(), partnerId)) {
            log.warn("Unexpected offer, rejecting contract negotiation");
            newRecord = negotiationRecordService.updateNegotiationRecordToState(newRecord.getOwnPid(), NegotiationState.TERMINATED);
            return new ResponseRecord(createErrorResponse(newRecord.getOwnPid().toString(), consumerPid, "ContractNegotiationError",
                    List.of("Unexpected offer, rejecting contract negotiation")), 400);
        }

        try {
            UUID assetId = UUID.fromString(targetAssetId);
            if (dataManagementService.getByIdForProperties(assetId, partnerProperties) == null) {
                log.warn("Unknown target asset id: {}", targetAssetId);
                newRecord = negotiationRecordService.updateNegotiationRecordToState(newRecord.getOwnPid(), NegotiationState.TERMINATED);
                return new ResponseRecord(createErrorResponse(newRecord.getOwnPid().toString(), consumerPid, "ContractNegotiationError",
                        List.of("Unknown target asset id: " + targetAssetId)), 400);
            }
        } catch (Exception e) {
            log.warn("Invalid target asset id: {}", targetAssetId);
            newRecord = negotiationRecordService.updateNegotiationRecordToState(newRecord.getOwnPid(), NegotiationState.TERMINATED);
            return new ResponseRecord(createErrorResponse(newRecord.getOwnPid().toString(), consumerPid, "ContractNegotiationError",
                    List.of("Invalid target asset id: " + targetAssetId)), 400);
        }

        String ackResponse = createResponse(newRecord);

        log.info("Negotiations Request endpoint received new partner request:\n {}", prettyPrint(requestJson));

        log.debug("Sending Response:\n{}", prettyPrint(ackResponse));

        executorService.submit(new SendContractAgreedTask(newRecord.getOwnPid(), negotiationRecordService, restClient,
                envService, dspTokenProviderService, policyService));

        return new ResponseRecord(ackResponse.getBytes(StandardCharsets.UTF_8), 201);
    }

    private static String createResponse(NegotiationRecord entry) {
        String type = entry.getState().equals(NegotiationState.TERMINATED) ?
                "dspace:ContractNegotiationError" : "dspace:ContractNegotiation";
        return Json.createObjectBuilder()
                .add("@context", FULL_CONTEXT)
                .add("@type", type)
                .add("dspace:providerPid", entry.getOwnPid().toString())
                .add("dspace:consumerPid", entry.getConsumerPid())
                .add("dspace:state", "dspace:" + entry.getState())
                .build()
                .toString();
    }

    /**
     * This method handles new incoming contract verification requests from the
     * /dsp/negotiations/{providerPid}/agreement/verification endpoint.
     *
     * @param requestBody - the request body of the incoming message
     * @param partnerId - the id of the requesting party as retrieved from the HTTP auth token
     * @return - code 200, if successful
     */
    public ResponseRecord handleVerificationRequest(String requestBody, String partnerId, UUID providerPid) {
        JsonObject requestJson = parseAndExpand(requestBody);
        String messageType = requestJson.getJsonArray("@type").getString(0);

        String consumerPid = requestJson.getJsonArray(DSPACE_NAMESPACE + "consumerPid").getJsonObject(0).getString("@value");
        String providerBodyPid = requestJson.getJsonArray(DSPACE_NAMESPACE + "providerPid").getJsonObject(0).getString("@value");
        NegotiationRecord existingRecord = negotiationRecordService.findByNegotiationRecordId(providerPid);

        if (existingRecord == null) {
            log.warn("Unknown provider negotiation id: {}", providerPid);
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Unknown provider negotiation id: " + providerPid)), 400);
        }

        if (!existingRecord.getState().equals(NegotiationState.AGREED)) {
            log.warn("Negotiation record expected in state AGREED, but found {}", existingRecord.getState());
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Negotiation record expected in state AGREED, but found " + existingRecord.getState())), 400);
        }

        if (!providerPid.toString().equals(providerBodyPid)) {
            negotiationRecordService.updateNegotiationRecordToState(providerPid, NegotiationState.TERMINATED);
            log.warn("Contradictory provider negotiation id: {} vs {}", providerPid, providerBodyPid);
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Contradictory provider negotiation id: " + providerPid + " vs " + providerBodyPid)), 400);
        }

        if (!messageType.equals(DSPACE_NAMESPACE + "ContractAgreementVerificationMessage")) {
            negotiationRecordService.updateNegotiationRecordToState(providerPid, NegotiationState.TERMINATED);
            log.warn("Wrong message type: {} at /negotiations/{providerPid}/agreement/verification", messageType);
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Wrong message type: ", messageType)), 400);
        }

        if (!existingRecord.getPartnerId().equals(partnerId)) {
            negotiationRecordService.updateNegotiationRecordToState(providerPid, NegotiationState.TERMINATED);
            log.warn("Contradictory client id: {} vs {}", existingRecord.getPartnerId(), partnerId);
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Contradictory client id: " + existingRecord.getPartnerId() + " vs " + partnerId)), 400);
        }


        existingRecord = negotiationRecordService.updateNegotiationRecordToState(providerPid, NegotiationState.VERIFIED);

        if (existingRecord != null) {
            // Initiate FINALIZED Message
            executorService.submit(new SendContractFinalizedTask(providerPid, negotiationRecordService, restClient,
                    dspTokenProviderService));
            return new ResponseRecord(null, 200);
        }
        return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                List.of("Internal Error")), 500);
    }
}
