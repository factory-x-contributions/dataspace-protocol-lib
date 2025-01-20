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
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.service.AuthorizationService;
import org.factoryx.library.connector.embedded.provider.service.TransferRecordService;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.FULL_CONTEXT;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.getSimpleCredential;

@Slf4j
/**
 * This class represents a task to send a "TransferStarted" message in the
 * context of a specific transfer request. Should be initiated, after a valid transfer
 * request has been received by a consumer partner.
 *
 * @author dalmasoud
 *
 */
public class SendTransferStartedTask implements Runnable {
    private final UUID transferId;
    private final TransferRecordService transferRecordService;
    private final AuthorizationService authorizationService;
    private final RestClient restClient;
    private final EnvService envService;

    public SendTransferStartedTask(UUID transferId, TransferRecordService transferRecordService,
                                   AuthorizationService authorizationService,
                                   RestClient restClient, EnvService envService) {
        this.transferId = transferId;
        this.transferRecordService = transferRecordService;
        this.authorizationService = authorizationService;
        this.restClient = restClient;
        this.envService = envService;
    }

    @Override
    public void run() {
        TransferRecord transferRecord = transferRecordService.findByTransferRecordId(transferId);
        if (transferRecord == null) {
            log.warn("Unknown transfer process record: {}", transferId);
            log.warn("Aborting send transfer started task.");
            return;
        }

        UUID datasetId = transferRecord.getDatasetId();

        String datasetUrl = envService.getDatasetUrl(datasetId);

        log.info("Starting transfer process {} for dataset {}", transferId, datasetId);

        TransferRecord transferRecordUpdated = transferRecordService.startTransferRecord(transferId,
                datasetUrl);

        String targetURL = transferRecord.getPartnerDspUrl() + "/transfers/" + transferRecord.getConsumerPid()
                + "/start";
        String requestBody = buildTransferStartedMessage(transferRecordUpdated);
        restClient
                .post()
                .uri(targetURL)
                .header("Content-Type", "application/json")
                .header("Authorization", getSimpleCredential(transferRecord.getPartnerDspUrl(), envService.getBackendId()))
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
    }

    private String buildTransferStartedMessage(TransferRecord record) {
        String datasetAddressUrl = record.getDatasetAddressUrl();
        String contractId = record.getContractId();
        String dataAccessToken = authorizationService.issueDataAccessToken(contractId, datasetAddressUrl);

        JsonObject authorization = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "authorization")
                .add("dspace:value", dataAccessToken)
                .build();
        JsonObject authType = Json.createObjectBuilder()
                .add("@type", "dspace:EndpointProperty")
                .add("dspace:name", "authType")
                .add("dspace:value", "Bearer")
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

        JsonObject dataAddress = Json.createObjectBuilder()
                .add("@type", "dspace:DataAddress")
                .add("dspace:endpointType", "https://w3id.org/idsa/v4.1/HTTP")
                .add("dspace:endpoint", record.getDatasetAddressUrl())
                .add("dspace:endpointProperties",
                        Json.createArrayBuilder().add(authorization).add(authType)
                                .add(authQuickFix).add(endpointQuickFix).add(authTypeFix).build())
                .build();
        return Json.createObjectBuilder()
                .add("@context", FULL_CONTEXT)
                .add("@type", "dspace:TransferStartMessage")
                .add("dspace:consumerPid", record.getConsumerPid())
                .add("dspace:providerPid", record.getOwnPid().toString())
                .add("dspace:dataAddress", dataAddress)
                .build()
                .toString();
    }
}
