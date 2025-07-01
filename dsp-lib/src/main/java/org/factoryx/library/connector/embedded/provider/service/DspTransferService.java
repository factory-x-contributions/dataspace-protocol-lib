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
import org.factoryx.library.connector.embedded.provider.model.ResponseRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferState;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
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
     * @param requestBody - the request body of the incoming message
     * @param partnerId - the id of the requesting party as retrieved from the HTTP auth token
     * @return - a response indicating the initiation status of the transfer process
     */
    public ResponseRecord handleNewTransfer(String requestBody, String partnerId, Map<String, String> partnerProperties) {
        JsonObject requestJson = parseAndExpand(requestBody);
        log.info("RequestJson: {}", requestJson);
        String consumerPid = requestJson.getJsonArray(DSPACE_NAMESPACE + "consumerPid").getJsonObject(0)
                .getString("@value");
        String agreementIdString = requestJson.getJsonArray(DSPACE_NAMESPACE + "agreementId").getJsonObject(0)
                .getString("@value");
        String messageType = requestJson.getJsonArray("@type").getString(0);
        String partnerDspUrl = requestJson.getJsonArray(DSPACE_NAMESPACE + "callbackAddress").getJsonObject(0)
                .getString("@value");

        TransferRecord newRecord = transferRecordService.createTransferRecord(consumerPid, partnerId,
                partnerDspUrl, agreementIdString);

        UUID agreementId;
        try {
            agreementId = UUID.fromString(agreementIdString);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID string for agreementId: {}", agreementIdString);
            return abortTransferWithBadRequest(newRecord, "Invalid agreementId");
        }

        NegotiationRecord negotiationRecord = transferRecordService
                .findNegotiationRecordByAgreementId(agreementId);
        if (negotiationRecord == null) {
            log.warn("Unknown negotiation record for transfer process: {}", agreementId);
            return abortTransferWithBadRequest(newRecord, "Unknown agreement ID");
        }

        if (!negotiationRecord.getState().equals(NegotiationState.FINALIZED)) {
            log.warn("Negotiation record is not in FINALIZED state: {}", negotiationRecord.getState());
            return abortTransferWithBadRequest(newRecord, "Agreement record is not in FINALIZED state");
        }

        String datasetIdString = negotiationRecord.getTargetAssetId();
        UUID datasetId;
        try {
            datasetId = UUID.fromString(datasetIdString);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID string for datasetId: {}", datasetIdString);
            return abortTransferWithBadRequest(newRecord, "Invalid dataset ID");
        }

        log.info("Received transfer request for datasetId: {}", datasetId);
        DataAsset dataset = dataManagementService.getByIdForProperties(datasetId, partnerProperties);
        if (dataset == null) {
            log.warn("Unknown dataset id {} for transfer record", datasetId);
            return abortTransferWithBadRequest(newRecord, "Unknown dataset");
        }

        newRecord = transferRecordService.addDatasetToTransferRecord(newRecord.getOwnPid(), datasetId);

        if (!messageType.equals(DSPACE_NAMESPACE + "TransferRequestMessage")) {
            newRecord = transferRecordService.updateTransferRecordState(newRecord.getOwnPid(),
                    TransferState.TERMINATED);
            return abortTransferWithBadRequest(newRecord, "Invalid message type");
        }

        byte[] ackResponse = createResponse(newRecord);

        log.info("TransferProcess Request endpoint received new partner request:\n {}",
                prettyPrint(requestJson));

        log.debug("Sending Response:\n{}", prettyPrint(new String(ackResponse)));

        executorService.submit(new SendTransferStartedTask(newRecord.getOwnPid(), transferRecordService,
                authorizationService, restClient, envService, dspTokenProviderService));

        return new ResponseRecord(ackResponse, 201);
    }

    private static byte[] createResponse(TransferRecord entry) {
        String type = entry.getState().equals(TransferState.TERMINATED) ? "dspace:TransferError"
                : "dspace:TransferProcess";
        return Json.createObjectBuilder()
                .add("@context", FULL_CONTEXT)
                .add("@type", type)
                .add("dspace:providerPid", entry.getOwnPid().toString())
                .add("dspace:consumerPid", entry.getConsumerPid())
                .add("dspace:state", "dspace:" + entry.getState())
                .build()
                .toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    private ResponseRecord abortTransferWithBadRequest(TransferRecord transferRecord, String errorMessage) {
        log.warn("Terminating transfer process with ID {} due to error: {}", transferRecord.getOwnPid(), errorMessage);
        transferRecordService.updateTransferRecordState(transferRecord.getOwnPid(), TransferState.TERMINATED);
        return new ResponseRecord(
                createErrorResponse(transferRecord.getOwnPid().toString(), transferRecord.getConsumerPid(),
                        "TransferError",
                        List.of(errorMessage)),
                400);
    }

    /**
     * This method handles incoming transfer completion requests from the
     * /dsp/transfers/{providerPid}/completion endpoint.
     *
     * @param requestBody
     * @param partnerId
     * @param providerPid
     * @return
     */
    public ResponseRecord handleCompletionRequest(String requestBody, String partnerId,
                                                  UUID providerPid) {
        TransferRecord transferRecord = transferRecordService.updateTransferRecordState(providerPid,
                TransferState.COMPLETED);

        if (transferRecord != null) {
            return new ResponseRecord(createResponse(transferRecord), 200);
        }
        return new ResponseRecord(createResponse(transferRecord), 400);
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
