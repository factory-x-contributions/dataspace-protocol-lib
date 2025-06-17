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

package org.factoryx.library.connector.embedded.service.policies;

import jakarta.json.Json;

import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.service.helpers.EnvService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * This class is meant to be used in the current MVD dataspace. It overrides the
 * createOfferedPolicy method in order to enforce the usage of the standard policy
 * of that dataspace.
 *
 * The validation is simply handled via the default implementation.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.policyservice", havingValue = "mvd")
public class MvdPolicyService extends DspPolicyService {

    public MvdPolicyService(EnvService envService) {
        super(envService);
    }

    @Override
    public JsonValue getObligation(String assetId, String partnerId) {
        var obligationsObject = Json.createObjectBuilder();
        obligationsObject.add("odrl:action", Json.createObjectBuilder().add(ID, "odrl:use").build());

        var constraintsObject = Json.createObjectBuilder();
        constraintsObject.add("odrl:leftOperand", Json.createObjectBuilder().add(ID, "DataAccess.level").build());
        constraintsObject.add("odrl:operator", Json.createObjectBuilder().add(ID, "odrl:eq").build());
        constraintsObject.add("odrl:rightOperand", "processing");

        obligationsObject.add("odrl:constraint", constraintsObject.build());
        return obligationsObject.build();
    }
}
