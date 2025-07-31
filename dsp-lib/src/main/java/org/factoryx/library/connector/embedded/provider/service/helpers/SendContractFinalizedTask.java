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
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.service.NegotiationRecordService;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.*;

/**
 * This class represents a task to send a "Finalized" ContractEvent message in the
 * context of a specific negotiation. Should be initiated, after a verification
 * request has been received by a consumer partner.
 *
 * @author eschrewe
 *
 */
@Slf4j
public class SendContractFinalizedTask implements Runnable {

    private final UUID negotiationId;
    private final NegotiationRecordService negotiationRecordService;
    private final RestClient restClient;
    private final DspTokenProviderService dspTokenProviderService;
    private final DspVersion dspVersion;

    public SendContractFinalizedTask(UUID negotiationId, NegotiationRecordService negotiationRecordService,
                                     RestClient restClient, DspTokenProviderService dspTokenProviderService, DspVersion dspVersion) {
        this.negotiationId = negotiationId;
        this.negotiationRecordService = negotiationRecordService;
        this.restClient = restClient;
        this.dspTokenProviderService = dspTokenProviderService;
        this.dspVersion = dspVersion;
    }

    @Override
    public void run() {
        log.info("Contract finalized task started with version {}", dspVersion);
        NegotiationRecord negotiationRecord = negotiationRecordService.findByNegotiationRecordId(negotiationId);
        if (negotiationRecord == null) {
            log.warn("Unknown negotiation record: {}", negotiationId);
            log.warn("Aborting send contract agreed task");
            return;
        }

        if (!negotiationRecord.getState().equals(NegotiationState.VERIFIED)) {
            log.warn("Expected negotiation record in state VERIFIED, but found {}", negotiationRecord.getState());
            log.warn("Aborting negotiation record with id {}", negotiationId);
            negotiationRecord.setState(NegotiationState.TERMINATED);
            return;
        }

        String targetURL = negotiationRecord.getPartnerDspUrl() + "/negotiations/" + negotiationRecord.getConsumerPid() + "/events";
        String requestBody = buildContractFinalizedMessage(negotiationRecord);
        log.debug("Created contract finalized request body: {}", requestBody);
        try {
            var response = restClient
                    .post()
                    .uri(targetURL)
                    .header("Content-Type", "application/json")
                    .header("Authorization", dspTokenProviderService.provideTokenForPartner(negotiationRecord))
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, resp) -> {
                                log.warn("Error sending FINALIZED MESSAGE to {} for {}", targetURL, negotiationId);
                                log.warn("Status code: {}", resp.getStatusCode());
                            })
                    .onStatus(HttpStatusCode::is2xxSuccessful,
                            (request, resp) -> {
                                log.info("Successfully sent FINALIZED MESSAGE to {} for {}!", targetURL, negotiationId);
                                negotiationRecordService.updateNegotiationRecordToState(negotiationId, NegotiationState.FINALIZED);
                            })
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private String buildContractFinalizedMessage(NegotiationRecord record) {
        String prefix = DspVersion.V_08.equals(dspVersion) ? "dspace:" : "";
        String eventPrefix = DspVersion.V_08.equals(dspVersion) ? "https://w3id.org/dspace/v0.8/" : "";
        return Json.createObjectBuilder()
                .add("@context", JsonUtils.getContextForDspVersion(dspVersion))
                .add("@type", prefix + "ContractNegotiationEventMessage")
                .add(prefix + "consumerPid", record.getConsumerPid())
                .add(prefix + "providerPid", record.getOwnPid().toString())
                .add(prefix + "eventType", eventPrefix + "FINALIZED")
                .build()
                .toString();
    }

}
