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
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAssetManagementService;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.model.ResponseRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.ContractRequestMessage;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.ContractVerificationMessage;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.NegotiationTerminationMessage;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
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
     * @param contractRequestMessage - the request body of the incoming message
     * @param partnerId              - the id of the requesting party as retrieved from the HTTP auth token
     * @param dspVersion             - the protocol version under which the request was sent
     * @return - a response ACK-body and code 201, if successful
     */
    public ResponseRecord handleNewNegotiation(ContractRequestMessage contractRequestMessage, String partnerId,
                                               Map<String, String> partnerProperties, DspVersion dspVersion) {
        String consumerPid = contractRequestMessage.getConsumerPid();
        String targetAssetId = contractRequestMessage.getTargetAssetId();
        JsonObject offer = contractRequestMessage.getOffer();
        NegotiationRecord newRecord = negotiationRecordService.createNegotiationRecord(consumerPid, partnerId,
                contractRequestMessage.getPartnerDspUrl(), targetAssetId);

        if (offer != null && !policyService.validateOffer(offer, newRecord.getTargetAssetId(), partnerId, dspVersion)) {
            log.warn("Unexpected offer, rejecting contract negotiation");
            log.info("Request: {}", prettyPrint(contractRequestMessage.getOffer()));
            newRecord = negotiationRecordService.updateNegotiationRecordToState(newRecord.getOwnPid(), NegotiationState.TERMINATED);
            return new ResponseRecord(createErrorResponse(newRecord.getOwnPid().toString(), consumerPid, "ContractNegotiationError",
                    List.of("Unexpected offer, rejecting contract negotiation"), dspVersion), 400);
        }

        try {
            UUID assetId = UUID.fromString(targetAssetId);
            if (dataManagementService.getByIdForProperties(assetId, partnerProperties) == null) {
                log.warn("Unknown target asset id: {}", targetAssetId);
                newRecord = negotiationRecordService.updateNegotiationRecordToState(newRecord.getOwnPid(), NegotiationState.TERMINATED);
                return new ResponseRecord(createErrorResponse(newRecord.getOwnPid().toString(), consumerPid, "ContractNegotiationError",
                        List.of("Unknown target asset id: " + targetAssetId), dspVersion), 400);
            }
        } catch (Exception e) {
            log.warn("Invalid target asset id: {}", targetAssetId);
            newRecord = negotiationRecordService.updateNegotiationRecordToState(newRecord.getOwnPid(), NegotiationState.TERMINATED);
            return new ResponseRecord(createErrorResponse(newRecord.getOwnPid().toString(), consumerPid, "ContractNegotiationError",
                    List.of("Invalid target asset id: " + targetAssetId), dspVersion), 400);
        }

        String ackResponse = createResponse(newRecord, dspVersion);

        log.info("Sending Response:\n{}", prettyPrint(ackResponse));

        executorService.submit(new SendContractAgreedTask(newRecord.getOwnPid(), negotiationRecordService, restClient,
                envService, dspTokenProviderService, policyService, dspVersion));

        return new ResponseRecord(ackResponse.getBytes(StandardCharsets.UTF_8), 201);
    }

    private static String createResponse(NegotiationRecord entry, DspVersion dspVersion) {
        String prefix = DspVersion.V_08.equals(dspVersion) ? "dspace:" : "";
        return Json.createObjectBuilder()
                .add("@context", JsonUtils.getContextForDspVersion(dspVersion))
                .add("@type", prefix + "ContractNegotiation")
                .add(prefix + "providerPid", entry.getOwnPid().toString())
                .add(prefix + "consumerPid", entry.getConsumerPid())
                .add(prefix + "state", prefix + entry.getState())
                .build()
                .toString();
    }

    /**
     * This method handles new incoming contract verification requests from the
     * /dsp/negotiations/{providerPid}/agreement/verification endpoint.
     *
     * @param contractVerificationMessage - the request body of the incoming message
     * @param partnerId                   - the id of the requesting party as retrieved from the HTTP auth token
     * @return - code 200, if successful
     */
    public ResponseRecord handleVerificationRequest(ContractVerificationMessage contractVerificationMessage, String partnerId,
                                                    UUID providerPid, DspVersion version) {

        String consumerPid = contractVerificationMessage.getConsumerPid();
        String providerBodyPid = contractVerificationMessage.getProviderPid();
        NegotiationRecord existingRecord = negotiationRecordService.findByNegotiationRecordId(providerPid);

        if (existingRecord == null) {
            log.warn("Unknown provider negotiation id: {}", providerPid);
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Unknown provider negotiation id: " + providerPid), version), 400);
        }

        if (!existingRecord.getState().equals(NegotiationState.AGREED)) {
            log.warn("Negotiation record expected in state AGREED, but found {}", existingRecord.getState());
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Negotiation record expected in state AGREED, but found " + existingRecord.getState()), version), 400);
        }

        if (!providerPid.toString().equals(providerBodyPid)) {
            negotiationRecordService.updateNegotiationRecordToState(providerPid, NegotiationState.TERMINATED);
            log.warn("Contradictory provider negotiation id: {} vs {}", providerPid, providerBodyPid);
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Contradictory provider negotiation id: " + providerPid + " vs " + providerBodyPid), version), 400);
        }

        if (!existingRecord.getPartnerId().equals(partnerId)) {
            negotiationRecordService.updateNegotiationRecordToState(providerPid, NegotiationState.TERMINATED);
            log.warn("Contradictory client id: {} vs {}", existingRecord.getPartnerId(), partnerId);
            return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                    List.of("Contradictory client id: " + existingRecord.getPartnerId() + " vs " + partnerId), version), 400);
        }

        existingRecord = negotiationRecordService.updateNegotiationRecordToState(providerPid, NegotiationState.VERIFIED);

        if (existingRecord != null) {
            // Initiate FINALIZED Message
            executorService.submit(new SendContractFinalizedTask(providerPid, negotiationRecordService, restClient,
                    dspTokenProviderService, version));
            log.info("Responding with code 200 to verification request");
            return new ResponseRecord(null, 200);
        }
        return new ResponseRecord(createErrorResponse(providerPid.toString(), consumerPid, "ContractNegotiationError",
                List.of("Internal Error"), version), 500);
    }

    public ResponseRecord handleNegotiationTerminationRequest(NegotiationTerminationMessage terminationMessage, String partnerId, DspVersion version) {
        NegotiationRecord existingRecord = negotiationRecordService.findByNegotiationRecordId(terminationMessage.getProviderPid());
        if (existingRecord == null || !existingRecord.getConsumerPid().equals(terminationMessage.getConsumerPid()) ||
                !existingRecord.getPartnerId().equals(partnerId)) {
            return new ResponseRecord(createErrorResponse(terminationMessage.getProviderPid().toString(), terminationMessage.getConsumerPid(),
                    "ContractNegotiationError", List.of("Unknown Negotiation"), version), 400);
        }
        if (existingRecord.getState().equals(NegotiationState.FINALIZED)) {
            return new ResponseRecord(createErrorResponse(terminationMessage.getProviderPid().toString(), terminationMessage.getConsumerPid(),
                    "ContractNegotiationError", List.of("Can't terminate finalized negotiation"), version), 400);
        }
        existingRecord = negotiationRecordService.updateNegotiationRecordToState(
                terminationMessage.getProviderPid(), NegotiationState.TERMINATED);
        return new ResponseRecord(createResponse(existingRecord, version).getBytes(StandardCharsets.UTF_8), 200);

    }

    public ResponseRecord handleGetNegotiationStatusRequest(UUID providerPid, String partnerId, DspVersion version) {
        NegotiationRecord existingRecord = negotiationRecordService.findByNegotiationRecordId(providerPid);
        if (existingRecord == null || !existingRecord.getPartnerId().equals(partnerId)) {
            return new ResponseRecord(createErrorResponse(providerPid.toString(), null,
                    "ContractNegotiationError", List.of("Unknown Negotiation"), version), 400);
        }
        return new ResponseRecord(createNegotiationStatusResponse(existingRecord, version).getBytes(StandardCharsets.UTF_8), 200);
    }

    private String createNegotiationStatusResponse(NegotiationRecord negotiationRecord, DspVersion version) {
        return Json.createObjectBuilder()
                .add("@context", getContextForDspVersion(version))
                .add("@type", "ContractNegotiation")
                .add("providerPid", negotiationRecord.getOwnPid().toString())
                .add("consumerPid", negotiationRecord.getConsumerPid())
                .add("state", negotiationRecord.getState().toString())
                .build()
                .toString();

    }
}
