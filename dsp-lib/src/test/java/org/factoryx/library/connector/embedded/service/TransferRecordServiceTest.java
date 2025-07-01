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

package org.factoryx.library.connector.embedded.service;

import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferState;
import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepository;
import org.factoryx.library.connector.embedded.provider.service.ContractRecordService;
import org.factoryx.library.connector.embedded.provider.service.TransferRecordFactory;
import org.factoryx.library.connector.embedded.provider.service.TransferRecordService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransferRecordServiceTest {

    @Mock
    private TransferRecordRepository repository;

    @Mock
    private TransferRecordFactory transferRecordFactory;

    @Mock
    private ContractRecordService contractRecordService;

    @Mock
    private EnvService envService;

    @InjectMocks
    private TransferRecordService transferRecordService;

    private static final String CONSUMER_PID = UUID.randomUUID().toString();
    private static final String PARTNER_ID = "consumer";
    private static final String PARTNER_DSP_URL = "http://localhost:9080/protocol";
    private static final String AGREEMENT_ID = UUID.randomUUID().toString();
    private static final UUID DATASET_UUID = UUID.randomUUID();
    private static final UUID TARGET_ASSET_ID = UUID.randomUUID();
    private static final String DATASET_URL = "http://localhost:8080/dataset/" +
            TARGET_ASSET_ID;
    private static final UUID RECORD_ID = UUID.randomUUID();

    private TransferRecord mockRecord;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockRecord = new TransferRecord(){};
        mockRecord.setOwnPid(RECORD_ID);
        mockRecord.setConsumerPid(CONSUMER_PID);
        mockRecord.setPartnerId(PARTNER_ID);
        mockRecord.setPartnerDspUrl(PARTNER_DSP_URL);
        mockRecord.setContractId(AGREEMENT_ID);
        mockRecord.setState(TransferState.REQUESTED);
        when(repository.save(any(TransferRecord.class))).thenReturn(mockRecord);
        when(transferRecordFactory.create()).thenReturn(new TransferRecord(){});
    }
  

    @Test
    public void testCreateTransferRecord() {
        // Act
        TransferRecord createdRecord = transferRecordService.createTransferRecord(
                CONSUMER_PID, PARTNER_ID, PARTNER_DSP_URL, AGREEMENT_ID);

        // Assert
        assertNotNull(createdRecord);
        assertEquals(CONSUMER_PID, createdRecord.getConsumerPid());
        assertEquals(PARTNER_ID, createdRecord.getPartnerId());
        assertEquals(TransferState.REQUESTED, createdRecord.getState());
        assertEquals(AGREEMENT_ID, createdRecord.getContractId());
        verify(repository, times(1)).save(any(TransferRecord.class));
    }

    @Test
    public void testUpdateTransferRecordState() {
        // Arrange
        when(repository.findById(mockRecord.getOwnPid())).thenReturn((Optional) Optional.of(mockRecord));
        // Act
        TransferRecord updatedRecord = transferRecordService.updateTransferRecordState(mockRecord.getOwnPid(),
                TransferState.STARTED);

        // Assert
        assertNotNull(updatedRecord, "Expected updated record to be not null");
        assertEquals(TransferState.STARTED, updatedRecord.getState(), "Expected state to be updated");
        verify(repository, times(1)).save(any(TransferRecord.class));
    }

    @Test
    public void testAddDatasetToTransferRecord() {
        // Arrange
        when(repository.findById(mockRecord.getOwnPid())).thenReturn((Optional) Optional.of(mockRecord));
        when(repository.save(any(TransferRecord.class))).thenReturn(mockRecord);

        // Act
        TransferRecord updatedRecord = transferRecordService.addDatasetToTransferRecord(mockRecord.getOwnPid(),
                DATASET_UUID);

        // Assert
        assertNotNull(updatedRecord, "Expected updated record to be not null");
        assertEquals(DATASET_UUID, updatedRecord.getDatasetId(), "Expected datasetId to be updated");
        verify(repository, times(1)).save(any(TransferRecord.class));
    }

    @Test
    public void testAddDatasetToTransferRecord_TransferRecordNotFound() {
        // Arrange
        when(repository.findById(mockRecord.getOwnPid())).thenReturn(Optional.empty());
        // Act
        TransferRecord updatedRecord = transferRecordService.addDatasetToTransferRecord(mockRecord.getOwnPid(),
                DATASET_UUID);

        // Assert
        assertNull(updatedRecord, "Expected record to be null when TransferRecord is not found");
        verify(repository, times(0)).save(any(TransferRecord.class));
    }

    @Test
    public void testStartTransferRecord() {
        // Arrange
        when(repository.findById(mockRecord.getOwnPid())).thenReturn((Optional) Optional.of(mockRecord));
        when(repository.save(any(TransferRecord.class))).thenReturn(mockRecord);
        mockRecord.setState(TransferState.REQUESTED);

        // Act
        TransferRecord updatedRecord = transferRecordService.startTransferRecord(mockRecord.getOwnPid(), DATASET_URL);

        // Assert
        assertNotNull(updatedRecord, "Expected updated record to be not null");
        assertEquals(TransferState.STARTED, updatedRecord.getState(), "Expected state to be STARTED");
        assertEquals(DATASET_URL, updatedRecord.getDatasetAddressUrl(), "Expected dataset URL to be set");
        verify(repository, times(1)).save(any(TransferRecord.class));
    }

    @Test
    public void testStartTransferRecord_TransferRecordNotFound() {
        // Arrange
        when(repository.findById(mockRecord.getOwnPid())).thenReturn(Optional.empty());

        // Act
        TransferRecord updatedRecord = transferRecordService.startTransferRecord(mockRecord.getOwnPid(), DATASET_URL);

        // Assert
        assertNull(updatedRecord, "Expected record to be null when TransferRecord is not found");
        verify(repository, times(0)).save(any(TransferRecord.class));
    }

    @Test
    public void testStartTransferRecord_InvalidStateTransition() {
        // Arrange
        when(repository.findById(mockRecord.getOwnPid())).thenReturn((Optional) Optional.of(mockRecord));
        mockRecord.setState(TransferState.STARTED);

        // Act
        TransferRecord updatedRecord = transferRecordService.startTransferRecord(mockRecord.getOwnPid(), DATASET_URL);

        // Assert
        assertNull(updatedRecord, "Expected record to be null when state transition is invalid");
        verify(repository, times(0)).save(any(TransferRecord.class));
    }
}
