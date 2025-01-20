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
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferState;
import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepository;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class TransferRecordService {

    private final TransferRecordRepository repository;
    private final ContractRecordService contactRecordService;
    private final EnvService envService;

    public TransferRecordService(TransferRecordRepository repository,
                                 ContractRecordService contractRecordService, EnvService envService) {
        this.repository = repository;
        this.contactRecordService = contractRecordService;
        this.envService = envService;
    }

    /**
     * This method stores a new transfer to the database. Our own
     * processId will be auto-generated and should be retrieved from the
     * return object of this method.
     *
     * @param consumerPid   - the process id on the consumer side
     * @param partnerId     - the id under which the consumer refers to himself
     * @param partnerDspUrl - the DSP protocol URL of the consumer partner
     * @return - the created TransferRecord
     */
    public TransferRecord createTransferRecord(String consumerPid, String partnerId, String partnerDspUrl,
                                               String agreementId) {
        TransferRecord transferRecord = new TransferRecord();
        transferRecord.setConsumerPid(consumerPid);
        transferRecord.setPartnerId(partnerId);
        transferRecord.setPartnerDspUrl(partnerDspUrl);
        transferRecord.setContractId(agreementId);
        transferRecord.setFormat("HTTP_PULL");
        transferRecord.setState(TransferState.REQUESTED);
        return repository.save(transferRecord);
    }

    /**
     * Retrieves a TransferRecord for the given id
     *
     * @param transferRecordId - the id of the required TransferRecord
     * @return - the TransferRecord with the given id
     */
    public TransferRecord findByTransferRecordId(UUID transferRecordId) {
        return repository.findById(transferRecordId).orElse(null);
    }

    public NegotiationRecord findNegotiationRecordByAggreementId(UUID agreementId) {
        return contactRecordService.findByContractId(agreementId);
    }

    /**
     * Adds a dataset to a TransferRecord
     *
     * @param transferRecordId - the id of the TransferRecord to update
     * @param datasetId        - the id of the dataset to add
     * @return - the updated TransferRecord
     */
    public TransferRecord addDatasetToTransferRecord(UUID transferRecordId, UUID datasetId) {
        TransferRecord transferRecord = findByTransferRecordId(transferRecordId);
        if (transferRecord == null) {
            log.error("Update failed, unknown TransferRecord id: {}", transferRecordId);
            return null;
        }
        transferRecord.setDatasetId(datasetId);
        return repository.save(transferRecord);
    }

    /**
     * Updates the state of a TransferRecord
     *
     * @param transferRecordId - the id of the TransferRecord to update
     * @param newState         - the new state of the TransferRecord
     * @return - the updated TransferRecord
     */
    public TransferRecord updateTransferRecordState(UUID transferRecordId, TransferState newState) {
        TransferRecord transferRecord = findByTransferRecordId(transferRecordId);
        if (transferRecord == null) {
            log.error("Update failed, unknown TransferRecord id: {}", transferRecordId);
            return null;
        }

        boolean isValidTransition = validateStateTransition(transferRecord.getState(), newState);
        if (!isValidTransition) {
            log.error("Update failed, invalid state transition from {} to {}", transferRecord.getState(), newState);
            return null;
        }

        transferRecord.setState(newState);
        return repository.save(transferRecord);
    }

    /**
     * Validates the state transition of a TransferRecord
     *
     * @param currentState - the current state of the TransferRecord
     * @param newState     - the new state of the TransferRecord
     * @return - true if the state transition is valid, false otherwise
     */
    private boolean validateStateTransition(TransferState currentState, TransferState newState) {
        return switch (currentState) {
            case REQUESTED, SUSPENDED -> newState == TransferState.STARTED || newState == TransferState.TERMINATED;
            case STARTED -> newState == TransferState.SUSPENDED || newState == TransferState.COMPLETED
                    || newState == TransferState.TERMINATED;
            default -> false;
        };
    }

    /**
     * Starts a transfer process by updating the state of the TransferRecord
     *
     * @param transferRecordId - the id of the TransferRecord to update
     * @param datasetUrl       - the URL of the dataset
     * @return - the updated TransferRecord
     */
    public TransferRecord startTransferRecord(UUID transferRecordId, String datasetUrl) {
        TransferRecord transferRecord = findByTransferRecordId(transferRecordId);
        if (transferRecord == null) {
            log.error("Update failed, unknown TransferRecord id: {}", transferRecordId);
            return null;
        }

        boolean isValidTransition = validateStateTransition(transferRecord.getState(), TransferState.STARTED);
        if (!isValidTransition) {
            log.error("Update failed, invalid state transition from {} to {}", transferRecord.getState(),
                    TransferState.STARTED);
            return null;
        }
        transferRecord.setState(TransferState.STARTED);
        transferRecord.setDatasetAddressUrl(datasetUrl);
        return repository.save(transferRecord);
    }
}
