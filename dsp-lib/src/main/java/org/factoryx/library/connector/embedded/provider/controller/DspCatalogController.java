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

import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.service.DspCatalogService;
import org.factoryx.library.connector.embedded.provider.service.deserializers.DeserializerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.prettyPrint;

@RestController
@Slf4j
/**
 * Endpoint for receiving DSP catalog requests
 * @author tobias.urbanek
 */
public class DspCatalogController {

    private final DspCatalogService dspCatalogService;
    private final DeserializerService deserializerService;
    private final DspTokenValidationService dspTokenValidationService;

    public DspCatalogController(DspCatalogService dspCatalogService, DeserializerService deserializerService,
                                DspTokenValidationService dspTokenValidationService) {
        this.dspCatalogService = dspCatalogService;
        this.deserializerService = deserializerService;
        this.dspTokenValidationService = dspTokenValidationService;
    }

    /**
     * Endpoint for requesting the catalog under DSP V.08.
     *
     * @param body  the request body as a String
     * @param authString the Authorization header
     * @return a ResponseEntity with the JSON response and HTTP status code 200
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/catalog/request")
    public ResponseEntity<String> catalogRequestV_08(@RequestBody(required = false) String body,
                                                     @RequestHeader(value = "Authorization", required = false) String authString) {
        return handleCatalogRequest(body, authString, DspVersion.V_08);
    }

    /**
     * Endpoint for requesting the catalog under DSP 2024/1
     *
     * @param body  the request body as a String
     * @param authString the Authorization header
     * @return a ResponseEntity with the JSON response and HTTP status code 200
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2024/1/catalog/request")
    public ResponseEntity<String> catalogRequestV_2024_1(@RequestBody(required = false) String body,
                                                     @RequestHeader(value = "Authorization", required = false) String authString) {
        return handleCatalogRequest(body, authString, DspVersion.V_2024_1);
    }

    /**
     * Endpoint for requesting the catalog under DSP 2025/1.
     *
     * @param body  the request body as a String
     * @param authString the Authorization header
     * @return a ResponseEntity with the JSON response and HTTP status code 200
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/catalog/request")
    public ResponseEntity<String> catalogRequestV_2025(@RequestBody(required = false) String body,
                                                     @RequestHeader(value = "Authorization", required = false) String authString) {
        return handleCatalogRequest(body, authString, DspVersion.V_2025_1);
    }

    private ResponseEntity<String> handleCatalogRequest(String requestBody, String authString, DspVersion version) {
        // Check if body or token is null
        if (requestBody == null || deserializerService.deserializeCatalogRequestMessage(requestBody, version) == null
                || authString == null || authString.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request: Body and Authorization token are required.");
        }

        try {
            log.info("Starting token validation");
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            log.info("Got Result from token validation: {}", partnerId);
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            JsonObject jsonResponse = dspCatalogService.getFullCatalog(partnerId, tokenValidationResult, version);
            return ResponseEntity.status(HttpStatus.OK).body(jsonResponse.toString());

        } catch (Exception e) {
            // Handle any unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing the request.");
        }
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/catalog/datasets/{id}")
    public ResponseEntity<String> datasetRequestV_08(@RequestHeader(value = "Authorization") String authString,
                                                     @PathVariable UUID id) {
        return handleDatasetRequest(authString, id, DspVersion.V_08);
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/2024/1/catalog/datasets/{id}")
    public ResponseEntity<String> datasetRequestV_2024_1(@RequestHeader(value = "Authorization") String authString,
                                                     @PathVariable UUID id) {
        return handleDatasetRequest(authString, id, DspVersion.V_2024_1);
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/catalog/datasets/{id}")
    public ResponseEntity<String> datasetRequestV_2025_1(@RequestHeader(value = "Authorization") String authString,
                                                         @PathVariable UUID id) {
        return handleDatasetRequest(authString, id, DspVersion.V_2025_1);
    }

    private ResponseEntity<String> handleDatasetRequest(String authString, UUID id, DspVersion version) {
        try {
            log.info("Starting token validation");
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            log.info("Got Result from token validation: {}", partnerId);
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            var datasetResponse = dspCatalogService.getDataset(partnerId, tokenValidationResult, id, version);
            if (datasetResponse != null) {
                return ResponseEntity.status(HttpStatus.OK).body(datasetResponse.toString());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing the request.");
    }
}
