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


import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAssetManagementService;
import org.factoryx.library.connector.embedded.provider.service.helpers.DataAccessTokenValidationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


/**
 *
 * Default Endpoint for servicing DSP-data-pull requests
 *
 * @author eschrewe
 */
@RestController
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.usebuiltindataccess", havingValue = "true", matchIfMissing = true)
public class DataAccessController {

    private final DataAssetManagementService dataAssetManagementService;
    private final DataAccessTokenValidationService dataAccessTokenValidationService;

    public DataAccessController(DataAssetManagementService dataAssetManagementService, DataAccessTokenValidationService dataAccessTokenValidationService) {
        this.dataAssetManagementService = dataAssetManagementService;
        this.dataAccessTokenValidationService = dataAccessTokenValidationService;
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/data-access/{assetId}")
    public ResponseEntity<byte[]> dataAccess(@RequestHeader("Authorization") String authToken, @PathVariable("assetId") String assetId) {
        boolean tokenValidation = dataAccessTokenValidationService.validateDataAccessTokenForAssetId(authToken, assetId);
        if (!tokenValidation) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var asset = dataAssetManagementService.getById(assetId);
        if (asset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType())).body(asset.getDtoRepresentation());
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/data-access/{assetId}/{*path}")
    public ResponseEntity<byte[]> forwardPostRequest(@RequestHeader("Authorization") String authToken, @PathVariable("assetId") String assetId,
                                                     @PathVariable String path, @RequestBody byte[] body, @RequestHeader HttpHeaders incomingHeaders,
                                                     @RequestParam MultiValueMap<String, String> incomingQueryParams) {
        return forwardApiAssetRequest(assetId, HttpMethod.POST, authToken, path, body, incomingHeaders, incomingQueryParams);
    }

    @PutMapping("${org.factoryx.library.dspapiprefix:/dsp}/data-access/{assetId}/{*path}")
    public ResponseEntity<byte[]> forwardPutRequest(@RequestHeader("Authorization") String authToken, @PathVariable("assetId") String assetId,
                                                    @PathVariable String path, @RequestBody byte[] body, @RequestHeader HttpHeaders incomingHeaders,
                                                    @RequestParam MultiValueMap<String, String> incomingQueryParams) {
        return forwardApiAssetRequest(assetId, HttpMethod.PUT, authToken, path, body, incomingHeaders, incomingQueryParams);
    }

    @DeleteMapping("${org.factoryx.library.dspapiprefix:/dsp}/data-access/{assetId}/{*path}")
    public ResponseEntity<byte[]> forwardDeleteRequest(@RequestHeader("Authorization") String authToken, @PathVariable("assetId") String assetId,
                                                       @PathVariable String path, @RequestHeader HttpHeaders incomingHeaders,
                                                       @RequestParam MultiValueMap<String, String> incomingQueryParams) {
        return forwardApiAssetRequest(assetId, HttpMethod.DELETE, authToken, path, null, incomingHeaders, incomingQueryParams);
    }

    private ResponseEntity<byte[]> forwardApiAssetRequest(String assetId, HttpMethod method, String authToken, String path, byte[] body,
                                                          HttpHeaders incomingHeaders, MultiValueMap<String, String> incomingQueryParams) {
        boolean tokenValidation = dataAccessTokenValidationService.validateWriteAccessTokenForAssetId(authToken, assetId.toString());
        if (!tokenValidation) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return dataAssetManagementService.forwardToApiAsset(assetId, method, body, incomingHeaders, path, incomingQueryParams);
    }
}