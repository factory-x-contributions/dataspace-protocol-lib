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
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.service.NegotiationRecordService;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService.isEmpty;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.prettyPrint;

/**
 * This class represents a task to send a ContractAgreed message in the
 * context of a specific negotiation. Should be initiated, after an agreeable
 * new contract request has been received by a consumer partner.
 *
 * @author eschrewe
 */
@Slf4j
public class SendContractAgreedTask implements Runnable {
    private final UUID negotiationId;
    private final NegotiationRecordService negotiationRecordService;
    private final RestClient restClient;
    private final EnvService envService;
    private final DspTokenProviderService dspTokenProviderService;
    private final DspPolicyService dspPolicyService;
    private final DspVersion dspVersion;

    public SendContractAgreedTask(UUID negotiationId, NegotiationRecordService negotiationRecordService, RestClient restClient,
                                  EnvService envService, DspTokenProviderService dspTokenProviderService, DspPolicyService dspPolicyService,
                                  DspVersion dspVersion) {
        this.negotiationId = negotiationId;
        this.negotiationRecordService = negotiationRecordService;
        this.restClient = restClient;
        this.envService = envService;
        this.dspTokenProviderService = dspTokenProviderService;
        this.dspPolicyService = dspPolicyService;
        this.dspVersion = dspVersion;
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
        log.debug("Created contract agreed request body: \n{}", prettyPrint(requestBody));


        try {
            restClient
                    .post()
                    .uri(targetURL)
                    .header("Content-Type", "application/json")
                    .header("Authorization", dspTokenProviderService.provideTokenForPartner(negotiationRecord))
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, resp) -> {
                                log.warn("Error sending AGREED MESSAGE to {} for {}", targetURL, negotiationId);
                                log.warn("Status code: {}", resp.getStatusCode());
                                var respBytes = resp.getBody().readAllBytes();
                                try {
                                    log.warn("Response from consumer\n{}", prettyPrint(new String(respBytes)));
                                } catch (Exception e) {
                                    log.warn("Response from consumer\n{}", new String(respBytes));
                                    log.warn("As bytes: {}", Arrays.toString(respBytes));
                                }
                                log.warn("Body: \n{}", prettyPrint(requestBody));
                            })
                    .onStatus(HttpStatusCode::is2xxSuccessful,
                            (request, resp) -> {
                                log.info("Successfully sent AGREED MESSAGE to {} for {}!", targetURL, negotiationId);
                                log.info("Body: \n{}", prettyPrint(requestBody));
                            })
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static String getTimestampForZuluTimeZone() {
        return DATE_TIME_FORMATTER.format(Instant.now().atZone(ZoneId.of("Z")));
    }

    private String buildContractAgreedMessage(NegotiationRecord record) {
        String odrlPrefix = dspVersion.ordinal() < DspVersion.V_2025_1.ordinal() ? "odrl:" : "";
        String dspacePrefix = dspVersion.ordinal() < DspVersion.V_2025_1.ordinal() ? "dspace:" : "";
        var agreementBuilder = Json.createObjectBuilder()
                .add("@type", odrlPrefix + "Agreement")
                .add("@id", record.getContractId().toString())
                .add(odrlPrefix + "target", record.getTargetAssetId())
                .add(dspacePrefix + "timestamp", getTimestampForZuluTimeZone())
                .add(odrlPrefix + "assignee", record.getPartnerId())
                .add(odrlPrefix + "assigner", envService.getBackendId());
        var permission = dspPolicyService.getPermission(record.getTargetAssetId(), record.getPartnerId(), dspVersion);
        if (permission != null && !isEmpty(permission)) {
            agreementBuilder.add(odrlPrefix + "permission", permission);
        }
        var prohibition = dspPolicyService.getProhibition(record.getTargetAssetId(), record.getPartnerId(), dspVersion);
        if (prohibition != null && !isEmpty(prohibition)) {
            agreementBuilder.add(odrlPrefix + "prohibition", prohibition);
        }
        var obligation = dspPolicyService.getObligation(record.getTargetAssetId(), record.getPartnerId(), dspVersion);
        if (obligation != null && !isEmpty(prohibition)) {
            agreementBuilder.add(odrlPrefix + "obligation", obligation);
        }
        var builder = Json.createObjectBuilder()
                .add("@context", JsonUtils.getContextForDspVersion(dspVersion))
                .add("@type", dspacePrefix + "ContractAgreementMessage")
                .add(dspacePrefix + "consumerPid", record.getConsumerPid())
                .add(dspacePrefix + "providerPid", record.getOwnPid().toString())
                .add(dspacePrefix + "agreement", agreementBuilder);
        if (dspVersion.ordinal() < DspVersion.V_2025_1.ordinal()) {
            builder.add(dspacePrefix + "callbackAddress", envService.getOwnDspUrl());
        }
        return builder.build().toString();
    }
}
