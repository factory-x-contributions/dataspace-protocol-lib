/*
 * Copyright (c) 2026. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.factoryx.library.connector.embedded.provider.service.dsp_validation.fxvalidation_v0_1;

import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.Nonnull;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.service.dsp_validation.base.BaseTokenProviderService;
import org.factoryx.library.connector.embedded.provider.service.dsp_validation.base.BaseDcpValidationService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.parse;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.prettyPrint;

@Slf4j
public class FXv0_1_ValidationService extends BaseDcpValidationService {

    FXv0_1_ValidationService(RestClient restClient, EnvService envService, BaseTokenProviderService baseTokenProviderService, Environment environment) {
        super(restClient, envService, baseTokenProviderService, environment);
    }

    @Override
    @Nonnull
    protected Map<String, String> evaluateVerifiablePresentations(List<SignedJWT> verifiablePresentations, String partnerDid) {
        Map<String, String> result = new HashMap<>();
        for (var p : verifiablePresentations) {
            try {
                JsonObject presentationPayload = parse(p.getPayload().toString());
                log.info("Found presentation payload: {}", prettyPrint(presentationPayload));
                result.put(ReservedKeys.credentials.toString(), "dataspacemember");
            } catch (Exception e){

            }
        }
        return result;
    }
}
