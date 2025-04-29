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

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.validationservice", havingValue = "mock", matchIfMissing = true)
public class MockValidationService implements DspTokenValidationService {

    private final EnvService envService;

    public MockValidationService(EnvService envService) {
        this.envService = envService;
    }

    @Override
    public String validateToken(String token) {
        try {
            var authJson = JsonUtils.parse(token);
            String clientId = authJson.getString("clientId");
            String audience = authJson.getString("audience");
            if (envService.getOwnDspUrl().equals(audience)) {
                return clientId;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Failure while validating token {}", token, e);
            return null;
        }
    }

    @Override
    public String validateRefreshToken(String token) {
        try {
            var authJson = JsonUtils.parse(token);
            String issuer = authJson.getString("iss");
            String audience = authJson.getString("audience");
            if (envService.getOwnDspUrl().equals(issuer)) {
                return audience;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Failure while validating refresh token {}", token, e);
            return null;
        }
    }
}
