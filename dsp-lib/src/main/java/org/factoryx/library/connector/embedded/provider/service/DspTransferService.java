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
import org.factoryx.library.connector.embedded.provider.interfaces.DataAsset;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAssetManagementService;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.model.ResponseRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferState;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.*;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.factoryx.library.connector.embedded.provider.service.helpers.SendTransferStartedTask;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.*;

/**
 * This service contains the logic for handling incoming transfer requests in
 * the DSP context.
 *
 * @author dalmasoud
 *
 */
@Service
@Slf4j
public class DspTransferService {

    private final TransferRecordService transferRecordService;

    private final DataAssetManagementService dataManagementService;

    private final AuthorizationService authorizationService;

    private final ExecutorService executorService;

    private final RestClient restClient;

    private final EnvService envService;

    private final DspTokenProviderService dspTokenProviderService;

    public DspTransferService(TransferRecordService transferRecordService,
                              DataAssetManagementService dataManagementService, AuthorizationService authorizationService,
                              ExecutorService executorService, RestClient restClient, EnvService envService,
                              DspTokenProviderService dspTokenProviderService) {
        this.transferRecordService = transferRecordService;
        this.dataManagementService = dataManagementService;
        this.authorizationService = authorizationService;
        this.executorService = executorService;
        this.restClient = restClient;
        this.envService = envService;
        this.dspTokenProviderService = dspTokenProviderService;
    }

    /**
     * This method handles new incoming transfer requests from the
     * /dsp/transfers/request endpoint.
     *
     * @param transferRequestMessage - the essentials of the request body of the incoming message
     * @param partnerId - the id of the requesting party as retrieved from the HTTP auth token
     * @return - a response indicating the initiation status of the transfer process
     */
    public ResponseRecord handleNewTransfer(TransferRequestMessage transferRequestMessage, String partnerId, Map<String, String> partnerProperties, DspVersion version) {

        String consumerPid = transferRequestMessage.getConsumerPid();
        String partnerDspUrl = transferRequestMessage.getPartnerDspUrl();
        UUID agreementId = transferRequestMessage.getAgreementId();
        TransferRecord newRecord = transferRecordService.createTransferRecord(consumerPid, partnerId,
                partnerDspUrl, agreementId.toString());

        NegotiationRecord negotiationRecord = transferRecordService
                .findNegotiationRecordByAgreementId(agreementId);
        if (negotiationRecord == null || !partnerId.equals(negotiationRecord.getPartnerId())) {
            log.warn("Unknown negotiation record for transfer process: {}", agreementId);
            return abortTransferWithBadRequest(newRecord, "Unknown agreement ID", version);
        }

        if (!negotiationRecord.getState().equals(NegotiationState.FINALIZED)) {
            log.warn("Negotiation record is not in FINALIZED state: {}", negotiationRecord.getState());
            return abortTransferWithBadRequest(newRecord, "Agreement record is not in FINALIZED state", version);
        }

        log.info("Received transfer request for datasetId: {}", negotiationRecord.getTargetAssetId());
        DataAsset dataset = dataManagementService.getByIdForProperties(negotiationRecord.getTargetAssetId(), partnerProperties);
        if (dataset == null) {
            log.warn("Unknown dataset id {} for transfer record", negotiationRecord.getTargetAssetId());
            return abortTransferWithBadRequest(newRecord, "Unknown dataset", version);
        }

        newRecord = transferRecordService.addDatasetToTransferRecord(newRecord.getOwnPid(), negotiationRecord.getTargetAssetId());

        byte[] ackResponse = createResponse(newRecord, version);

        log.debug("Sending Response:\n{}", prettyPrint(new String(ackResponse)));

        executorService.submit(new SendTransferStartedTask(newRecord.getOwnPid(), transferRecordService,
                authorizationService, restClient, envService, dspTokenProviderService, version, dataset));

        return new ResponseRecord(ackResponse, 201);
    }

    private static byte[] createResponse(TransferRecord entry, DspVersion version) {
        String prefix = DspVersion.V_08.equals(version) ? "dpace:" : "";
        String type = entry.getState().equals(TransferState.TERMINATED) ? prefix + "TransferError"
                : prefix +  "TransferProcess";
        return Json.createObjectBuilder()
                .add("@context", JsonUtils.getContextForDspVersion(version))
                .add("@type", type)
                .add(prefix + "providerPid", entry.getOwnPid().toString())
                .add(prefix + "consumerPid", entry.getConsumerPid())
                .add(prefix + "state", prefix + entry.getState())
                .build()
                .toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    private ResponseRecord abortTransferWithBadRequest(TransferRecord transferRecord, String errorMessage, DspVersion version) {
        log.warn("Terminating transfer process with ID {} due to error: {}", transferRecord.getOwnPid(), errorMessage);
        transferRecordService.updateTransferRecordState(transferRecord.getOwnPid(), TransferState.TERMINATED);
        return new ResponseRecord(
                createErrorResponse(transferRecord.getOwnPid().toString(), transferRecord.getConsumerPid(),
                        "TransferError",
                        List.of(errorMessage), version),
                400);
    }

    /**
     * This method handles incoming transfer completion requests from the
     * /dsp/transfers/{providerPid}/completion endpoint.
     *
     * @param transferCompletionMessage
     * @param partnerId
     * @param providerPid
     * @return
     */
    public ResponseRecord handleCompletionRequest(TransferCompletionMessage transferCompletionMessage, String partnerId,
                                                  UUID providerPid, DspVersion version) {
        TransferRecord transferRecord = transferRecordService.findByTransferRecordId(providerPid);
        if (transferRecord.getPartnerId().equals(partnerId) && transferCompletionMessage.getProviderPid().equals(providerPid)) {
            transferRecord = transferRecordService.updateTransferRecordState(providerPid, TransferState.COMPLETED);
            if (transferRecord != null) {
                return new ResponseRecord(createResponse(transferRecord, version), 200);
            }
        }

        return new ResponseRecord(createErrorResponse(transferCompletionMessage.getConsumerPid(), providerPid.toString(),
                "TransferError", List.of("Invalid completion request"), version), 400);
    }


    public ResponseRecord handleGetStatusRequest(UUID providerPid, String partnerId, DspVersion version) {
        TransferRecord transferRecord = transferRecordService.findByTransferRecordId(providerPid);
        if (transferRecord != null && transferRecord.getPartnerId().equals(partnerId)) {
            return new ResponseRecord(createStatusResponse(transferRecord, version).getBytes(StandardCharsets.UTF_8), 200);
        }
        return new ResponseRecord(createErrorResponse(providerPid.toString(), "unknown", "TransferError",
                List.of("Invalid transfer status request"), version), 400);
    }

    private String createStatusResponse(TransferRecord transferRecord, DspVersion version) {
        return Json.createObjectBuilder()
                .add("@context", JsonUtils.getContextForDspVersion(version))
                .add("@type", "TransferProcess")
                .add("providerPid", transferRecord.getOwnPid().toString())
                .add("consumerPid", transferRecord.getConsumerPid())
                .add("state", transferRecord.getState().toString())
                .build()
                .toString();
    }

    /**
     * This method handles incoming transfer termination requests from the
     * /dsp/transfers/{providerPid}/termination endpoint.
     *
     * @param terminationMessage
     * @param partnerId
     * @param providerPid
     * @return
     */
    public ResponseRecord handleTerminationRequest(TransferTerminationMessage terminationMessage, String partnerId,
                                                   UUID providerPid, DspVersion version) {
        TransferRecord transferRecord = transferRecordService.findByTransferRecordId(providerPid);
        if (transferRecord.getPartnerId().equals(partnerId) && terminationMessage.getProviderPid().equals(providerPid)) {
            transferRecord = transferRecordService.updateTransferRecordState(providerPid, TransferState.TERMINATED);
            if (transferRecord != null) {
                return new ResponseRecord(createResponse(transferRecord, version), 200);
            }
        }

        return new ResponseRecord(createErrorResponse(terminationMessage.getConsumerPid(), providerPid.toString(),
                "TransferError", List.of("Invalid termination request"), version), 400);
    }

    /**
     * This method handles incoming transfer termination requests from the
     * /dsp/transfers/{providerPid}/termination endpoint.
     *
     * @param suspensionMessage
     * @param partnerId
     * @param providerPid
     * @return
     */
    public ResponseRecord handleSuspensionRequest(TransferSuspensionMessage suspensionMessage, String partnerId,
                                                  UUID providerPid, DspVersion version) {
        TransferRecord transferRecord = transferRecordService.findByTransferRecordId(providerPid);
        if (transferRecord.getPartnerId().equals(partnerId) && suspensionMessage.getProviderPid().equals(providerPid)) {
            transferRecord = transferRecordService.updateTransferRecordState(providerPid, TransferState.SUSPENDED);
            if (transferRecord != null) {
                return new ResponseRecord(createResponse(transferRecord, version), 200);
            }
        }
        return new ResponseRecord(createErrorResponse(suspensionMessage.getConsumerPid(), providerPid.toString(),
                "TransferError", List.of("Invalid suspension request"), version), 400);
    }

    /**
     * This method handles incoming transfer termination requests from the
     * /dsp/transfers/{providerPid}/termination endpoint.
     *
     * @param startMessage
     * @param partnerId
     * @param providerPid
     * @return
     */
    public ResponseRecord handleStartRequest(TransferStartMessage startMessage, String partnerId,
                                             UUID providerPid, DspVersion version) {
        TransferRecord transferRecord = transferRecordService.findByTransferRecordId(providerPid);
        if (transferRecord.getPartnerId().equals(partnerId) && startMessage.getProviderPid().equals(providerPid)) {
            transferRecord = transferRecordService.updateTransferRecordState(providerPid, TransferState.STARTED);
            if (transferRecord != null) {
                return new ResponseRecord(createResponse(transferRecord, version), 200);
            }
        }
        return new ResponseRecord(createErrorResponse(startMessage.getConsumerPid(), providerPid.toString(),
                "TransferError", List.of("Invalid start request"), version), 400);
    }


    /**
     * This method handles incoming token refresh requests from the
     * /dsp/transfers/refresh endpoint.
     *
     * @param refreshToken - the refresh token
     * @return - a response containing the new access token and refresh token
     */
    public ResponseRecord handleRefreshTokenRequest(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return new ResponseRecord("Refresh token is required".getBytes(StandardCharsets.UTF_8), 400);
        }

        try {
            refreshToken = refreshToken.replace("Bearer ", "").replace("bearer ", "");

            var refreshTokenClaims = authorizationService.extractAllClaims(refreshToken);
            String accessToken = refreshTokenClaims.getStringClaim(AuthorizationService.TOKEN);
            String partnerId = refreshTokenClaims.getSubject();

            var accessTokenClaims = authorizationService.extractAllClaims(accessToken);
            String contractId = accessTokenClaims.getStringClaim(AuthorizationService.CONTRACT_ID);
            String datasetAddressUrl = accessTokenClaims.getStringClaim(AuthorizationService.DATA_ADDRESS);

            String newAccessToken = authorizationService.issueDataAccessToken(contractId, datasetAddressUrl);
            String newRefreshToken = authorizationService.issueRefreshToken(accessToken, partnerId);

            long expiresIn = 300;
            return new ResponseRecord(
                createRefreshTokenResponse(newRefreshToken, newAccessToken, expiresIn), 200);
        } catch (Exception e) {
            log.error("Error while handling refresh token request", e);
            return new ResponseRecord("Error while handling refresh token request".getBytes(StandardCharsets.UTF_8), 500);
        }
    
    }

    private static byte[] createRefreshTokenResponse(String refreshToken, String accessToken, long expiresIn) {
        return Json.createObjectBuilder()
                .add("access_token", accessToken)
                .add("token_type", "Bearer")
                .add("expires_in", expiresIn)
                .add("refresh_token", refreshToken)
                .build()
                .toString()
                .getBytes(StandardCharsets.UTF_8);
    }
}
