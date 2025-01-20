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
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.service.DspCatalogService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
/**
 * Endpoint for receiving DSP catalog requests
 * @author tobias.urbanek
 */
public class DspCatalogController {

    private final DspCatalogService dspCatalogService;

    public DspCatalogController(DspCatalogService dspCatalogService) {
        this.dspCatalogService = dspCatalogService;
    }

    /**
     * Endpoint for requesting the catalog.
     *
     * @param body  the request body as a String
     * @param token the Authorization header
     * @return a ResponseEntity with the JSON response and HTTP status code 200
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/catalog/request")
    public ResponseEntity<String> requestCatalog(@RequestBody(required = false) String body,
                                                 @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if body or token is null
        if (body == null || token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request: Body and Authorization token are required.");
        }

        try {
            List<JsonObject> catalogs = dspCatalogService.getAllCatalogs();
            JsonObject jsonResponse = dspCatalogService.buildFinalCatalogResponse(catalogs);
            return ResponseEntity.status(HttpStatus.OK).body(jsonResponse.toString());
        } catch (Exception e) {
            // Handle any unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing the request.");
        }
    }
}
