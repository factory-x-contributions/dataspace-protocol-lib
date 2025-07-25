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
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;

import java.util.UUID;

@Getter
@Setter
@Slf4j
@ToString
@Entity
public class JpaNegotiationRecord extends NegotiationRecord {
    // Initial attributes (these are not expected to change after the negotiation has started):

    /**
     * The process id, under which we ourselves identify an ongoing negotiation
     * <p>
     * Is always assigned by the service. Never set manually!
     */
    @Id
    @GeneratedValue()
    private UUID ownPid;
    /**
     * The process id, under which the other partner refers to
     * this negotiation
     */
     private String consumerPid;
    /**
     * The id, under which your partner refers to itself
     */
     private String partnerId;
    /**
     * The protocol URL of your partner
     */
     private String partnerDspUrl;
    /**
     * The id of the asset which is targeted by this negotiation
     */
     private String targetAssetId;
    /**
     * The credentials, which our partner has sent us during this negotiation
     */
     private String partnerCredentials;

    // Attributes that may be set later:
    /**
     * The current state of the negotiation
     */
    private NegotiationState state;
    /**
     * The id of the contract, if the negotiation reaches the AGREED status
     * <p>
     * Is always assigned by the service. Never set manually!
     */
     private UUID contractId;
}
