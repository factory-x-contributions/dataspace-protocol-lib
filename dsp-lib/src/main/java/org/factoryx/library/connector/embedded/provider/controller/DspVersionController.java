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

package org.factoryx.library.connector.embedded.provider.controller;

import org.factoryx.library.connector.embedded.provider.metadata.VersionEntry;
import org.factoryx.library.connector.embedded.provider.metadata.VersionResponse;
import org.factoryx.library.connector.embedded.provider.profile.Auth;
import org.factoryx.library.connector.embedded.provider.profile.DataspaceProfileContext;
import org.factoryx.library.connector.embedded.provider.profile.DataspaceProfileContextRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller exposing the DSP-compliant version discovery endpoint under /.well-known/dspace-version.
 * Returns a list of available protocol versions and their endpoints.
 *
 * @author tobias-urb
 */
@RestController
public class DspVersionController {

    private final DataspaceProfileContextRegistry registry;

    public DspVersionController(DataspaceProfileContextRegistry registry) {
        this.registry = registry;
    }

    /**
     * Handles GET requests to /.well-known/dspace-version.
     *
     * @return a JSON response containing all registered DSP version profiles
     */
    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/.well-known/dspace-version")
    public ResponseEntity<VersionResponse> getProtocolVersions() {
        return ResponseEntity.ok(new VersionResponse(
                registry.getAll().stream().map(this::mapToVersionEntry).toList()
        ));
    }

    /**
     * Maps an internal DataspaceProfileContext to a public-facing VersionEntry.
     *
     * @param context the internal context
     * @return version entry for client consumption
     */
    private VersionEntry mapToVersionEntry(DataspaceProfileContext context) {
        return new VersionEntry(
                context.getVersion(),
                context.getPath(),
                context.getBinding(),
                context.getServiceId(),
                context.getIdentifierType(),
                mapAuth(context.getAuth())
        );
    }

    /**
     * Converts internal Auth DTO to public metadata Auth.
     *
     * @param auth internal auth configuration
     * @return converted Auth or null
     */
    private Auth mapAuth(Auth auth) {
        if (auth == null) return null;
        return new Auth(
                auth.getProtocol(),
                auth.getVersion(),
                auth.getProfile()
        );
    }
}
