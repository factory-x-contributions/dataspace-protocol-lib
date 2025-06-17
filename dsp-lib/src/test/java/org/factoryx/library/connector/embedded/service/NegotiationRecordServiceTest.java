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

/**package org.factoryx.library.connector.embedded.service;

import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.factoryx.library.connector.embedded.provider.service.NegotiationRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class NegotiationRecordServiceTest {

    @Mock
    private NegotiationRecordRepository repository;

    @InjectMocks
    private NegotiationRecordService negotiationRecordService;

    private static final String CONSUMER_PID = UUID.randomUUID().toString();
    private static final String PARTNER_ID = "consumer";
    private static final String PARTNER_DSP_URL = "http://localhost:9080/protocol";
    private static final String TARGET_ASSET_ID = UUID.randomUUID().toString();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID CONTRACT_ID = UUID.randomUUID();
    private NegotiationRecord mockRecord;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockRecord = new NegotiationRecord();
        mockRecord.setOwnPid(RECORD_ID);
        mockRecord.setContractId(CONTRACT_ID);
        mockRecord.setConsumerPid(CONSUMER_PID);
        mockRecord.setPartnerId(PARTNER_ID);
        mockRecord.setPartnerDspUrl(PARTNER_DSP_URL);
        mockRecord.setTargetAssetId(TARGET_ASSET_ID);
        mockRecord.setState(NegotiationState.REQUESTED);

        when(repository.save(any(NegotiationRecord.class))).thenReturn(mockRecord);
        when(repository.findById(RECORD_ID)).thenReturn(Optional.of(mockRecord));
        when(repository.findAllByContractId(CONTRACT_ID)).thenReturn(Collections.singletonList(mockRecord));
    }

    @Test
    void testCreateNegotiationRecord() {
        // Act
        NegotiationRecord createdRecord = negotiationRecordService.createNegotiationRecord(CONSUMER_PID, PARTNER_ID, PARTNER_DSP_URL, TARGET_ASSET_ID);

        // Assert
        assertNotNull(createdRecord);
        assertEquals(CONSUMER_PID, createdRecord.getConsumerPid());
        assertEquals(PARTNER_ID, createdRecord.getPartnerId());
        assertEquals(NegotiationState.REQUESTED, createdRecord.getState());
    }

    @Test
    void testFindByNegotiationRecordId() {
        // Arrange
        when(repository.findById(RECORD_ID)).thenReturn(Optional.of(mockRecord));

        // Act
        NegotiationRecord foundRecord = negotiationRecordService.findByNegotiationRecordId(RECORD_ID);

        // Assert
        assertNotNull(foundRecord);
        assertEquals(RECORD_ID, foundRecord.getOwnPid());
    }

    @Test
    void testUpdateNegotiationRecordToState_Agreed() {
        // Act
        NegotiationRecord updatedRecord = negotiationRecordService.updateNegotiationRecordToState(RECORD_ID, NegotiationState.AGREED);

        // Assert
        assertNotNull(updatedRecord);
        assertEquals(NegotiationState.AGREED, updatedRecord.getState());
        assertNotNull(updatedRecord.getContractId());
    }

    @Test
    void testUpdateNegotiationRecordToState_Failed() {
        // Arrange
        when(repository.findById(RECORD_ID)).thenReturn(Optional.empty());

        // Act
        NegotiationRecord updatedRecord = negotiationRecordService.updateNegotiationRecordToState(RECORD_ID, NegotiationState.AGREED);

        // Assert
        assertNull(updatedRecord);  
    }

    @Test
    void testFindByContractId() {
        // Arrange	
        when(repository.findAllByContractId(CONTRACT_ID)).thenReturn(Collections.singletonList(mockRecord));
    
        // Act
        NegotiationRecord foundRecord = negotiationRecordService.findByContractId(CONTRACT_ID);
    
        // Assert
        assertNotNull(foundRecord);
        assertEquals(CONTRACT_ID, foundRecord.getContractId());  
    }

    @Test
    void testFindByContractId_MultipleRecords() {
        // Simulate multiple records with the same contractId
        NegotiationRecord anotherRecord = new NegotiationRecord();
        anotherRecord.setContractId(CONTRACT_ID);
        when(repository.findAllByContractId(CONTRACT_ID)).thenReturn(Arrays.asList(mockRecord, anotherRecord));

        // Act
        NegotiationRecord foundRecord = negotiationRecordService.findByContractId(CONTRACT_ID);

        // Assert
        assertNotNull(foundRecord);
        assertEquals(CONTRACT_ID, foundRecord.getContractId());
    }

}
**/