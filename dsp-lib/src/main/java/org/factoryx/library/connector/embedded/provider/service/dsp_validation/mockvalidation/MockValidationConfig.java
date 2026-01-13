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

package org.factoryx.library.connector.embedded.provider.service.dsp_validation.mockvalidation;

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is a fallback for simple test scenarios. It is intended to provide
 * simplified instances of the {@link DspTokenValidationService} and {@link DspTokenProviderService}
 * types, that can interact with EDC control planes that are using MockIAM.
 * <p>
 * In any other case, you should use a DCP-based Services, see
 * {@link org.factoryx.library.connector.embedded.provider.service.dsp_validation.fxvalidation_v0_1.FXValidationConfig}
 * as an example.
 */
@Configuration
@Slf4j
public class MockValidationConfig {

    @Bean
    @ConditionalOnMissingBean(DspTokenProviderService.class)
    public DspTokenProviderService mockTokenProviderService(EnvService env) {
        log.warn("Using MockTokenProviderService");
        return new MockTokenProviderService(env);
    }

    @Bean
    @ConditionalOnMissingBean(DspTokenValidationService.class)
    public DspTokenValidationService mockValidationService(EnvService env) {
        log.warn("Using MockTokenValidationService");
        return new MockValidationService(env);
    }
}
