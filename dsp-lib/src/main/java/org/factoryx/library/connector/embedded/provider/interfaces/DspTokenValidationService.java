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

import jakarta.json.JsonValue;

import java.util.Map;

public interface DspTokenValidationService {

    /**
     * Validates a token that was received at a DSP protocol endpoint.
     *
     * @param token the received token
     * @return a mapping containing the partnerId (if validated successfully), and optionally the partner's credentials
     *         and further optionally additional properties of that partner.
     *
     */
    Map<String, String> validateToken(String token);

    default JsonValue getAuthInfo() {
        return null;
    }

    default JsonValue getIdentifierTypeInfo() {
        return null;
    }

    enum ReservedKeys {
        /**
         * The id of the partner. In case of DCP based authentication, this should be the partner's did:web id.
         */
        partnerId,

        /**
         * The credentials found during the evaluation of the auth token from the partner. It may contain a single
         * word describing the found credential. Or it may contain a comma-separated list of credentials.
         */
        credentials,
    }
}
