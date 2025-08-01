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

package org.factoryx.library.connector.embedded.provider.profile;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes and registers default dataspace profiles at application startup.
 * This includes support for 0.8 and 2025-1 DSP versions.
 *
 * @author tobias-urb
 */
@Component
public class ProfileContextInitializer {
    private final DataspaceProfileContextRegistry registry;

    public ProfileContextInitializer(DataspaceProfileContextRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers built-in profiles after Spring context is initialized.
     */
    @PostConstruct
    public void init() {
        registry.register(new DataspaceProfileContext(
                "dsp-2025",
                "2025-1",
                "/dsp/2025-1",
                "HTTPS",
                "service-2025",
                "did:web",
                null,
                null
        ));

        registry.register(new DataspaceProfileContext(
                "dsp-08",
                "v0.8",
                "/dsp/0.8",
                "HTTPS",
                "service-08",
                "D-U-N-S",
                new Auth("OAuth", "2", List.of("authorization_code", "refresh_token")),
                "https://w3id.org/dspace/v0.8/"
        ));
    }
}
