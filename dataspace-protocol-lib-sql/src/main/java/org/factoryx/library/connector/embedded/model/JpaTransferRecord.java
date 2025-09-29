/*
 * Copyright (c) 2025. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.factoryx.library.connector.embedded.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferState;

import java.util.UUID;

@Getter
@Setter
@Slf4j
@ToString
@Entity
public class JpaTransferRecord extends TransferRecord {
    /**
     * The transfer id on the Provider side (our side)
     * <p>
     * Is always assigned by the service. Never set manually!
     */
    @Id
    @GeneratedValue()
    private UUID ownPid;

    /**
     * The transfer id on the Consumer side
     */
    private String consumerPid;

    /**
     * The id of the consumer partner
     */
    private String partnerId;

    /**
     * The URI indicating where messages to the Consumer should be sent
     */
    private String partnerDspUrl;

    /**
     * The credentials, which our partner has sent us during this transfer
     */
    private String partnerCredentials;

    /**
     * The id of the agreement, which is the basis for this transfer
     */
    private String contractId;

    /**
     * The id of the dataset, which is the subject of this transfer
     */
    private String datasetId;

    /**
     * The format of the transfer, usually "HTTP_PUSH" or "HTTP_PULL"
     */
    private String format;

    /**
     * The endpoint to which the data should be transferred
     */
    private String datasetAddressUrl;

    /**
     * The current state of the transfer
     */
   private TransferState state;
}
