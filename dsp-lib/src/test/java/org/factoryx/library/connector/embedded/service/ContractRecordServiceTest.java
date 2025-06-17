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
import org.factoryx.library.connector.embedded.provider.service.ContractRecordService;
import org.factoryx.library.connector.embedded.provider.service.NegotiationRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class ContractRecordServiceTest {

    @Mock
    private NegotiationRecordRepository repository;

    @InjectMocks
    private NegotiationRecordService negotiationRecordService;

    private ContractRecordService contractRecordService;

    private static final UUID CONTRACT_ID = UUID.randomUUID();
    private NegotiationRecord mockRecord;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        contractRecordService = negotiationRecordService;

        mockRecord = new NegotiationRecord();
        mockRecord.setOwnPid(UUID.randomUUID());
        mockRecord.setContractId(CONTRACT_ID);
        mockRecord.setState(NegotiationState.AGREED);
        when(repository.findAllByContractId(CONTRACT_ID))
                .thenReturn(Collections.singletonList(mockRecord));
    }

    @Test
    void testFindByContractId() {
        // Act
        NegotiationRecord record = contractRecordService.findByContractId(CONTRACT_ID);

        // Assert
        assertNotNull(record, "record should not be null");
        assertEquals(CONTRACT_ID, record.getContractId(),
                "ContractId should be the same as the one we used for the mock");
        assertEquals(NegotiationState.AGREED, record.getState(),
                "NegotiationRecord should be in AGREED state");
    }
}**/
