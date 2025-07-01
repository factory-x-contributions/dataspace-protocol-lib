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

package org.factoryx.library.connector.embedded.provider.interfaces;

import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;

public interface DspTokenProviderService {

    /**
     * Provides a token which can be provided to a partner as "Authorization" header in
     * a DSP protocol request message.
     *
     * @param record the record containing the relevant information about the ongoing process and the involved partner.
     * @return the token
     */
    String provideTokenForPartner(NegotiationRecord record);

    /**
     * Provides a token which can be provided to a partner as "Authorization" header in
     * a DSP protocol request message.
     *
     * @param record the record containing the relevant information about the ongoing process and the involved partner.
     * @return the token
     */
    String provideTokenForPartner(TransferRecord record);
}
