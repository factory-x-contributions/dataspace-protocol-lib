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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@RestController
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.usebuiltindataccess", havingValue = "true", matchIfMissing = true)
/**
 *
 * Default Endpoint for servicing DSP-data-pull requests
 *
 * @author eschrewe
 */
public class DataAccessController {

    private final DataAssetManagementService dataAssetManagementService;
    private final DataAccessTokenValidationService dataAccessTokenValidationService;

    @Autowired
    private RestTemplate restTemplate;

    public DataAccessController(DataAssetManagementService dataAssetManagementService, DataAccessTokenValidationService dataAccessTokenValidationService) {
        this.dataAssetManagementService = dataAssetManagementService;
        this.dataAccessTokenValidationService = dataAccessTokenValidationService;
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/data-access/{assetid}")
    public ResponseEntity<byte[]> dataAccess(@RequestHeader("Authorization") String authToken, @PathVariable("assetid") UUID assetId) {
        boolean tokenValidation = dataAccessTokenValidationService.validateDataAccessTokenForAssetId(authToken, assetId.toString());
        if (!tokenValidation) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var asset = dataAssetManagementService.getById(assetId);
        if (asset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType())).body(asset.getDtoRepresentation());
    }

    @GetMapping("/data/{*path}")
    public ResponseEntity<byte[]> forwardGetRequest(@RequestHeader("Authorization") String authToken, @PathVariable String path) {
        return forwardRequest(HttpMethod.GET, authToken, path, null);
    }

    @PostMapping("/data/{*path}")
    public ResponseEntity<byte[]> forwardPostRequest(@RequestHeader("Authorization") String authToken, @PathVariable String path, @RequestBody byte[] body) {
        return forwardRequest(HttpMethod.POST, authToken, path, body);
    }

    @PutMapping("/data/{*path}")
    public ResponseEntity<byte[]> forwardPutRequest(@RequestHeader("Authorization") String authToken, @PathVariable String path, @RequestBody byte[] body) {
        return forwardRequest(HttpMethod.PUT, authToken, path, body);
    }

    @DeleteMapping("/data/{*path}")
    public ResponseEntity<byte[]> forwardDeleteRequest(@RequestHeader("Authorization") String authToken, @PathVariable String path) {
        return forwardRequest(HttpMethod.DELETE, authToken, path, null);
    }

    private ResponseEntity<byte[]> forwardRequest(HttpMethod method, String authToken, String path, byte[] body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        String url = org.factoryx.library.aasdataaccess + path; // AAS Url + path variables
        ResponseEntity<byte[]> response = restTemplate.exchange(url, method, entity, byte[].class);

        return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(response.getBody());
    }
}