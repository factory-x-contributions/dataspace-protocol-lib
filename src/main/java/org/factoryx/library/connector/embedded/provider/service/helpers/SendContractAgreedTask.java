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
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.service.NegotiationRecordService;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.FULL_CONTEXT;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.getSimpleCredential;

/**
 * This class represents a task to send a ContractAgreed message in the
 * context of a specific negotiation. Should be initiated, after an agreeable
 * new contract request has been received by a consumer partner.
 */
@Slf4j
public class SendContractAgreedTask implements Runnable {
    private final UUID negotiationId;
    private final NegotiationRecordService negotiationRecordService;
    private final RestClient restClient;
    private final EnvService envService;


    public SendContractAgreedTask(UUID negotiationId, NegotiationRecordService negotiationRecordService, RestClient restClient,
                                  EnvService envService) {
        this.negotiationId = negotiationId;
        this.negotiationRecordService = negotiationRecordService;
        this.restClient = restClient;
        this.envService = envService;
    }

    @Override
    public void run() {
        NegotiationRecord negotiationRecord = negotiationRecordService.findByNegotiationRecordId(negotiationId);
        if (negotiationRecord == null) {
            log.warn("Unknown negotiation record: {}", negotiationId);
            log.warn("Aborting send contract agreed task");
            return;
        }

        if (!negotiationRecord.getState().equals(NegotiationState.REQUESTED)) {
            log.warn("Expected negotiation record in state REQUESTED, but found {}", negotiationRecord.getState());
            log.warn("Aborting negotiation record with id {}", negotiationId);
            negotiationRecord.setState(NegotiationState.TERMINATED);
            return;
        }

        negotiationRecord = negotiationRecordService.updateNegotiationRecordToState(negotiationId, NegotiationState.AGREED);

        String targetURL = negotiationRecord.getPartnerDspUrl() + "/negotiations/" + negotiationRecord.getConsumerPid() + "/agreement";
        String requestBody = buildContractAgreedMessage(negotiationRecord);
        log.debug("Created contract agreed request body: {}", requestBody);
        restClient
                .post()
                .uri(targetURL)
                .header("Content-Type", "application/json")
                .header("Authorization", getSimpleCredential(negotiationRecord.getPartnerDspUrl(), envService.getBackendId()))
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        (request, resp) -> {
                            log.warn("Error sending AGREED MESSAGE to {} for {}", targetURL, negotiationId);
                            log.warn("Status code: {}", resp.getStatusCode());
                            log.warn("Body: {}", requestBody);
                        })
                .onStatus(HttpStatusCode::is2xxSuccessful,
                        (request, resp) -> {
                            log.info("Successfully sent AGREED MESSAGE to {} for {}!", targetURL, negotiationId);
                        })
                .toBodilessEntity();
    }

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static String getTimestampForZuluTimeZone() {
        return DATE_TIME_FORMATTER.format(Instant.now().atZone(ZoneId.of("Z")));
    }

    private String buildContractAgreedMessage(NegotiationRecord record) {
        JsonObject agreementObject = Json.createObjectBuilder()
                .add("@type", "odrl:Agreement")
                .add("@id", record.getContractId().toString())
                .add("odrl:target", record.getTargetAssetId())
                .add("dspace:timestamp", getTimestampForZuluTimeZone())
                .add("odrl:assignee", "consumer")
                .add("odrl:assigner", envService.getBackendId())
                .add("odrl:permission", Json.createArrayBuilder().build())
                .add("odrl:prohibition", Json.createArrayBuilder().build())
                .add("odrl:obligation", Json.createArrayBuilder().build())
                .build();
        return Json.createObjectBuilder()
                .add("@context", FULL_CONTEXT)
                .add("@type", "dspace:ContractAgreementMessage")
                .add("dspace:consumerPid", record.getConsumerPid())
                .add("dspace:providerPid", record.getOwnPid().toString())
                .add("dspace:callbackAddress", envService.getOwnDspUrl())
                .add("dspace:agreement", agreementObject)
                .build()
                .toString();
    }
}
