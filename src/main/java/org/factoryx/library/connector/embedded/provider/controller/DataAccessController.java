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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.usebuiltindataccess", havingValue = "true")
public class DataAccessController {

    private final DataAssetManagementService dataAssetManagementService;
    private final DataAccessTokenValidationService dataAccessTokenValidationService;

    public DataAccessController(DataAssetManagementService dataAssetManagementService, DataAccessTokenValidationService dataAccessTokenValidationService) {
        this.dataAssetManagementService = dataAssetManagementService;
        this.dataAccessTokenValidationService = dataAccessTokenValidationService;
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/data-access/{assetid}")
    public ResponseEntity<byte[]> dataAccess(@RequestHeader("Authorization") String authToken, @PathVariable("assetid") UUID assetId) {
        boolean tokenValidation = dataAccessTokenValidationService.validateDataAccessTokenForAssetId(authToken, assetId.toString());
        if (!tokenValidation) {
            return ResponseEntity.status(401).build();
        }
        var asset = dataAssetManagementService.getById(assetId);
        if (asset == null) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType())).body(asset.getDtoRepresentation());
    }

}
