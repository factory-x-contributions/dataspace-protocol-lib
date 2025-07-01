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

package org.factoryx.library.connector.embedded.provider.service.policies;

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * This class purely uses the default implementations of the abstract DspPolicyService, i.e. it will offer
 * and accept only policies, where permissions, obligations and prohibitions are empty.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.policyservice", havingValue = "empty", matchIfMissing = true)
public class EmptyPolicyService extends DspPolicyService {

    public EmptyPolicyService(EnvService envService) {
        super(envService);
    }

}
