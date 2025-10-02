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

package org.factoryx.library.connector.embedded.provider.service.helpers;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.ApiAsset;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAsset;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.service.AuthorizationService;
import org.factoryx.library.connector.embedded.provider.service.TransferRecordService;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.LEGACY_CONTEXT;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.prettyPrint;


/**
 * This class represents a task to send a "TransferStarted" message in the
 * context of a specific transfer request. Should be initiated, after a valid transfer
 * request has been received by a consumer partner.
 *
 * @author dalmasoud
 */
@Slf4j
public class SendTransferStartedTask implements Runnable {
    private final UUID transferId;
    private final TransferRecordService transferRecordService;
    private final AuthorizationService authorizationService;
    private final RestClient restClient;
    private final EnvService envService;
    private final DspTokenProviderService dspTokenProviderService;
    private final DspVersion dspVersion;
    private final DataAsset dataAsset;


    public SendTransferStartedTask(UUID transferId, TransferRecordService transferRecordService,
                                   AuthorizationService authorizationService, RestClient restClient,
                                   EnvService envService, DspTokenProviderService dspTokenProviderService, DspVersion dspVersion, DataAsset dataAsset) {
        this.transferId = transferId;
        this.transferRecordService = transferRecordService;
        this.authorizationService = authorizationService;
        this.restClient = restClient;
        this.envService = envService;
        this.dspTokenProviderService = dspTokenProviderService;
        this.dspVersion = dspVersion;
        this.dataAsset = dataAsset;
    }

    @Override
    public void run() {
        TransferRecord transferRecord = transferRecordService.findByTransferRecordId(transferId);
        if (transferRecord == null) {
            log.warn("Unknown transfer process record: {}", transferId);
            log.warn("Aborting send transfer started task.");
            return;
        }

        String datasetId = transferRecord.getDatasetId();

        String datasetUrl = envService.getEdrEndpoint(dataAsset);

        log.info("Starting transfer process {} for dataset {} under version {}", transferId, datasetId, dspVersion);

        TransferRecord transferRecordUpdated = transferRecordService.startTransferRecord(transferId,
                datasetUrl);

        String targetURL = transferRecord.getPartnerDspUrl() + "/transfers/" + transferRecord.getConsumerPid()
                + "/start";
        String requestBody = switch (dspVersion) {
            case V_08 -> buildTransferStartedMessage_V_08(transferRecordUpdated, dataAsset);
            default -> buildTransferStartedMessage(transferRecordUpdated, dataAsset);
        };
        log.info("Generated TransferStartMessage: \n{}", prettyPrint(requestBody));
        try {
            restClient
                    .post()
                    .uri(targetURL)
                    .header("Content-Type", "application/json")
                    .header("Authorization", dspTokenProviderService.provideTokenForPartner(transferRecordUpdated))
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, resp) -> {
                                log.warn("Error sending STARTED MESSAGE to {} for {}",
                                        targetURL, transferId);
                                log.warn("Status code: {}", resp.getStatusCode());
                                log.warn("Body: {}", requestBody);
                            })
                    .onStatus(HttpStatusCode::is2xxSuccessful,
                            (request, resp) -> {
                                log.info("Successfully sent STARTED MESSAGE to {} for {}!",
                                        targetURL, transferId);
                            })
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    private String buildTransferStartedMessage(TransferRecord transferRecord, DataAsset asset) {
        try {
            String dataAccessToken = asset instanceof ApiAsset ?
                    authorizationService.issueWriteAccessToken(transferRecord.getContractId(), transferRecord.getDatasetId())
                    : authorizationService.issueDataAccessToken(transferRecord.getContractId(), transferRecord.getDatasetAddressUrl());
            JsonObjectBuilder message = Json.createObjectBuilder();
            message.add("@context", JsonUtils.getContextForDspVersion(dspVersion));
            message.add("@type", "TransferStartMessage");
            message.add("consumerPid", transferRecord.getConsumerPid());
            message.add("providerPid", transferRecord.getOwnPid().toString());

            JsonObjectBuilder dataAddress = Json.createObjectBuilder();
            dataAddress.add("@type", "DataAddress");
            dataAddress.add("endpointType", "https://w3id.org/idsa/v4.1/HTTP");
            dataAddress.add("endpoint", transferRecord.getDatasetAddressUrl());

            JsonArrayBuilder endpointProperties = Json.createArrayBuilder();
            JsonObjectBuilder authorizationProperty = Json.createObjectBuilder();
            authorizationProperty.add("@type", "EndpointProperty");
            authorizationProperty.add("name", "https://w3id.org/edc/v0.0.1/ns/authorization");
            authorizationProperty.add("value", dataAccessToken);
            JsonObjectBuilder authTypeProperty = Json.createObjectBuilder();
            authTypeProperty.add("@type", "EndpointProperty");
            authTypeProperty.add("name", "https://w3id.org/edc/v0.0.1/ns/authType");
            authTypeProperty.add("value", authorizationService.getAuthType());
            JsonObjectBuilder endpointProperty = Json.createObjectBuilder();
            endpointProperty.add("@type", "EndpointProperty");
            endpointProperty.add("name", "https://w3id.org/edc/v0.0.1/ns/endpoint");
            endpointProperty.add("value", transferRecord.getDatasetAddressUrl());
            endpointProperties.add(authorizationProperty);
            endpointProperties.add(authTypeProperty);
            endpointProperties.add(endpointProperty);
            dataAddress.add("endpointProperties", endpointProperties);

            message.add("dataAddress", dataAddress.build());
            return message.build().toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "{}";
    }


    private String buildTransferStartedMessage_V_08(TransferRecord record, DataAsset asset) {
        String datasetAddressUrl = record.getDatasetAddressUrl();
        String contractId = record.getContractId();
        String dataAccessToken = asset instanceof ApiAsset ?
                authorizationService.issueWriteAccessToken(contractId, record.getDatasetId())
                : authorizationService.issueDataAccessToken(contractId, datasetAddressUrl);
        String partnerId = record.getPartnerId();
        String refreshTokenValue = authorizationService.issueRefreshToken(dataAccessToken, partnerId);
        String expiresInValue = "300";
        String refreshEndpointValue = envService.getRefreshEndpoint();

        JsonObject authorization = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "authorization")
                .add("dspace:value", dataAccessToken)
                .build();
        JsonObject authType = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "authType")
                .add("dspace:value", authorizationService.getAuthType())
                .build();

        // The following properties are currently necessary for compatibility with the EDC connector
        JsonObject endpointQuickFix = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "https://w3id.org/edc/v0.0.1/ns/endpoint")
                .add("dspace:value", datasetAddressUrl)
                .build();
        JsonObject authQuickFix = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "https://w3id.org/edc/v0.0.1/ns/authorization")
                .add("dspace:value", dataAccessToken)
                .build();
        JsonObject authTypeFix = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "https://w3id.org/edc/v0.0.1/ns/authType")
                .add("dspace:value", "Bearer")
                .build();
        JsonObject refreshTokenProp = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "https://w3id.org/edc/v0.0.1/ns/refreshToken")
                .add("dspace:value", refreshTokenValue)
                .build();
        JsonObject expiresInProp = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "https://w3id.org/edc/v0.0.1/ns/expiresIn")
                .add("dspace:value", expiresInValue)
                .build();
        JsonObject refreshEndpointProp = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "https://w3id.org/edc/v0.0.1/ns/refreshEndpoint")
                .add("dspace:value", refreshEndpointValue)
                .build();

        JsonObject dataAddress = Json.createObjectBuilder()
                .add("@type", "dspace:DataAddress")
                .add("dspace:endpointType", "https://w3id.org/idsa/v4.1/HTTP")
                .add("dspace:endpoint", record.getDatasetAddressUrl())
                .add("dspace:endpointProperties",
                        Json.createArrayBuilder().add(authorization).add(authType)
                                .add(authQuickFix).add(endpointQuickFix).add(authTypeFix).add(refreshTokenProp)
                                .add(expiresInProp).add(refreshEndpointProp).build())
                .build();
        return Json.createObjectBuilder()
                .add("@context", LEGACY_CONTEXT)
                .add("@type", "dspace:TransferStartMessage")
                .add("dspace:consumerPid", record.getConsumerPid())
                .add("dspace:providerPid", record.getOwnPid().toString())
                .add("dspace:dataAddress", dataAddress)
                .build()
                .toString();
    }
}
