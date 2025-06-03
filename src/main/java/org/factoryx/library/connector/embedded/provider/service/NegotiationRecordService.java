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

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
/**
 * This service allows to persist, read and update NegotiationRecord entities
 *
 * @author eschrewe
 *
 */
public class NegotiationRecordService implements ContractRecordService {

    private final NegotiationRecordRepository repository;

    public NegotiationRecordService(NegotiationRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * This method stores a new negotiation to the database. Our own
     * processId will be auto-generated and should be retrieved from the
     * return object of this method.
     *
     * @param consumerPid - the process id on the consumer side
     * @param partnerId - the id under which the consumer refers to himself
     * @param partnerDspUrl - the DSP protocol URL of the consumer partner
     * @param targetAssetId - the id of the asset, which the consumer wants to gain access to
     * @return - the created NegotiationRecord
     */
    public NegotiationRecord createNegotiationRecord(String consumerPid, String partnerId, String partnerDspUrl,
                                                     String targetAssetId) {
        NegotiationRecord negotiationRecord = new NegotiationRecord();
        negotiationRecord.setOwnPid(UUID.randomUUID());
        negotiationRecord.setConsumerPid(consumerPid);
        negotiationRecord.setPartnerId(partnerId);
        negotiationRecord.setPartnerDspUrl(partnerDspUrl);
        negotiationRecord.setTargetAssetId(targetAssetId);
        negotiationRecord.setState(NegotiationState.REQUESTED);
        return repository.save(negotiationRecord);
    }

    /**
     * Retrieves a NegotiationRecord for the given id
     *
     * @param negotiationRecordId - the id of the required NegotiationRecord
     * @return - the NegotiationRecord, if it existed, else null
     */
    public NegotiationRecord findByNegotiationRecordId(UUID negotiationRecordId) {
        return repository.findById(negotiationRecordId).orElse(null);
    }

    /**
     * Sets the NegotiationRecord with the given id to a new state.
     * If the new state is AGREED, then a new unique contract id will be auto-generated.
     * It can be retrieved from the returned NegotiationRecord by then.
     *
     * @param negotiationRecordId - the id of the required NegotiationRecord
     * @param newState - the state, that this negotiation now has moved into
     * @return - the updated NegotiationRecord
     */
    public NegotiationRecord updateNegotiationRecordToState(UUID negotiationRecordId, NegotiationState newState) {
        NegotiationRecord existingRecord = findByNegotiationRecordId(negotiationRecordId);
        if (existingRecord != null) {
            if (NegotiationState.AGREED.equals(newState)) {
                // Assign new ContractId
                UUID contractId;
                do {
                    contractId = UUID.randomUUID();
                } while (!repository.findAllByContractId(contractId).isEmpty());
                existingRecord.setContractId(contractId);
            }
            existingRecord.setState(newState);
            return repository.save(existingRecord);
        } else {
            log.warn("Update failed, unknown NegotiationRecord id: {}", negotiationRecordId);
            return null;
        }
    }

    @Override
    public NegotiationRecord findByContractId(UUID contractId) {
        var result = repository.findAllByContractId(contractId);
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() > 1) {
            log.warn("Multiple Negotiation records found for contractId {}", contractId);
            log.warn("Arbitrarily returning the first record found!");
        }
        return result.getFirst();
    }
}
