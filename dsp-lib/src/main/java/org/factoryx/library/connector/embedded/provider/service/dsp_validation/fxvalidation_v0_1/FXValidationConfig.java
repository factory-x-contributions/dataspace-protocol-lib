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

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
import org.factoryx.library.connector.embedded.provider.service.dsp_validation.base.BaseTokenProviderService;
import org.factoryx.library.connector.embedded.provider.service.dsp_validation.base.DefaultDimWalletTokenProviderService;
import org.factoryx.library.connector.embedded.provider.service.dsp_validation.base.DefaultIdentityHubTokenProviderService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnExpression("'${org.factoryx.library.dcpvalidation:}'=='fxv0_1'")
@Slf4j
public class FXValidationConfig {


    @Bean
    public BaseTokenProviderService getTokenProviderService(RestClient restClient, Environment environment,
                                                            EnvService envService) {
        String apiProperty = environment.getProperty("org.factoryx.library.dcpvalidation.stsapi", "identityhub");
        log.info("stsapi api property is '{}'", apiProperty);
        if ("dim-wallet".equals(apiProperty)) {
            return new DefaultDimWalletTokenProviderService(restClient, environment, envService);
        }
        return new DefaultIdentityHubTokenProviderService(restClient, environment, envService);
    }

    @Bean
    @ConditionalOnBean(BaseTokenProviderService.class)
    public DspTokenValidationService fxTokenValidationService(RestClient restClient, EnvService envService,
                                                              BaseTokenProviderService baseTokenProviderService,
                                                              Environment environment) {
        return new FXv0_1_ValidationService(restClient, envService, baseTokenProviderService, environment);
    }


}
