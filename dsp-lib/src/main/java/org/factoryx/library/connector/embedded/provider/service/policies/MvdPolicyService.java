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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAsset;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
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
    public JsonArray getObligation(DataAsset dataAsset, String partnerId, DspVersion version) {
        String prefix = DspVersion.V_08.equals(version) ? "odrl:" : "";
        var obligationsObject = Json.createObjectBuilder();
        obligationsObject.add(prefix + "action", Json.createObjectBuilder().add(ID, prefix + "use").build());

        var constraintsObject = Json.createObjectBuilder();
        constraintsObject.add(prefix + "leftOperand", Json.createObjectBuilder().add(ID, "DataAccess.level").build());
        constraintsObject.add(prefix + "operator", Json.createObjectBuilder().add(ID, prefix + "eq").build());
        constraintsObject.add(prefix + "rightOperand", "processing");

        obligationsObject.add(prefix + "constraint", constraintsObject.build());
        return Json.createArrayBuilder().add(obligationsObject.build()).build();
    }
}
