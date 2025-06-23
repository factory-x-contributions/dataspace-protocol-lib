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

package org.factoryx.library.connector.embedded.provider.repository;

import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * Repository type for persisting NegotiationRecord entities
 *
 * @author eschrewe
 */
public interface NegotiationRecordRepository {

    List<NegotiationRecord> findAllByContractId(UUID contractId);

    NegotiationRecord save(NegotiationRecord record);

    Optional<? extends NegotiationRecord> findById(UUID id);
}
