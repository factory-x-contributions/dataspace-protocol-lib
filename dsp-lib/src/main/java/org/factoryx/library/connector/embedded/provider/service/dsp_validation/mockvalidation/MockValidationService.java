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

package org.factoryx.library.connector.embedded.provider.service.dsp_validation.mockvalidation;

import jakarta.json.JsonValue;
import jakarta.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.validationservice", havingValue = "mock", matchIfMissing = true)
public class MockValidationService implements DspTokenValidationService {

    private final EnvService envService;

    public MockValidationService(EnvService envService) {
        this.envService = envService;
    }

    @Override
    public JsonValue getAuthInfo() {
        return Json.createObjectBuilder().add("auth", Json.createObjectBuilder().add("protocol", "mock")).build();
    }

    @Override
    public JsonValue getIdentifierTypeInfo() {
        return Json.createObjectBuilder().add("identifierType", "did:web").build();
    }

    @Override
    public Map<String, String> validateToken(String token) {
        try {
            if ("Bearer ".equalsIgnoreCase(token.substring(0, 7))) {
                token = token.substring(7);
            }
            var authJson = JsonUtils.parse(token);
            String clientId = authJson.getString("clientId");
            String audience = authJson.getString("audience");
            if (envService.getOwnDspUrl().equals(audience)) {
                return Map.of(ReservedKeys.partnerId.toString(), clientId,
                        ReservedKeys.credentials.toString(), "dataspacemember");
            } else {
                return Map.of();
            }
        } catch (Exception e) {
            log.error("Failure while validating token {}", token, e);
            return Map.of();
        }
    }
}
