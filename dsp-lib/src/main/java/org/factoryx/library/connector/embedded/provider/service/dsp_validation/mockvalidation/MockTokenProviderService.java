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

import jakarta.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.validationservice", havingValue = "mock", matchIfMissing = true)
public class MockTokenProviderService implements DspTokenProviderService {

    private final EnvService envService;

    public MockTokenProviderService(EnvService envService) {
        this.envService = envService;
    }

    @Override
    public String provideTokenForPartner(NegotiationRecord record) {
        return getSimpleCredential(record.getPartnerDspUrl());
    }

    @Override
    public String provideTokenForPartner(TransferRecord record) {
        return getSimpleCredential(record.getPartnerDspUrl());
    }

    private String getSimpleCredential(String partnerDspUrl) {
        return Json.createObjectBuilder()
                .add("region", "eu")
                .add("audience", partnerDspUrl)
                .add("clientId", envService.getBackendId())
                .build()
                .toString();
    }
}
