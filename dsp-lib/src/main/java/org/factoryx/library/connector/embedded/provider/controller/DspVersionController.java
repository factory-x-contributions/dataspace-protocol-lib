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

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
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
@Slf4j
public class DspVersionController {

    private static String preparedResponse;

    private final DspTokenValidationService dspTokenValidationService;

    public DspVersionController(DspTokenValidationService dspTokenValidationService) {
        this.dspTokenValidationService = dspTokenValidationService;
    }

    /**
     * Handles GET requests to /.well-known/dspace-version.
     *
     * @return a JSON response containing all DSP version profiles
     */
    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/.well-known/dspace-version")
    public ResponseEntity<String> protocolVersions() {
        log.info("protocol versions request received");

        if (preparedResponse == null) {
            JsonValue authInfo = dspTokenValidationService.getAuthInfo();
            JsonValue identifierTypeInfo = dspTokenValidationService.getIdentifierTypeInfo();

            JsonArrayBuilder protocolVersionsArray = Json.createArrayBuilder()
                    .add(buildVersion("2025-1", "/2025/1", authInfo, identifierTypeInfo))
                    .add(buildVersion("v0.8", "/", authInfo, identifierTypeInfo));

            JsonObjectBuilder protocolVersions = Json.createObjectBuilder()
                    .add("protocolVersions", protocolVersionsArray);

            preparedResponse = protocolVersions.build().toString();
        }

        return ResponseEntity.ok().body(preparedResponse);
    }

    /**
     * Builds a JSON object representing a DSP protocol version.
     *
     * @param version the protocol version
     * @param path the path for the respective version
     * @param authInfo a JSON object containing authentication details
     * @param identifierTypeInfo a JSON object containing identifier type details
     * @return a JsonObjectBuilder containing all required and optional fields
     */
    private JsonObjectBuilder buildVersion(String version, String path,
                                           JsonValue authInfo, JsonValue identifierTypeInfo) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("version", version)
                .add("path", path)
                .add("binding", "HTTPS");

        if (authInfo != null) {
            builder.addAll(Json.createObjectBuilder(authInfo.asJsonObject()));
        }
        if (identifierTypeInfo != null) {
            builder.addAll(Json.createObjectBuilder(identifierTypeInfo.asJsonObject()));
        }

        return builder;
    }
}
