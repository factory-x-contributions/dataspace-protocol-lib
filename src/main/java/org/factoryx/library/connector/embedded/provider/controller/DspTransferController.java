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
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
import org.factoryx.library.connector.embedded.provider.model.ResponseRecord;
import org.factoryx.library.connector.embedded.provider.service.DspTransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Slf4j
/**
 * Endpoint for receiving DSP requests related to negotiations
 * 
 * @author dalmasoud
 */
public class DspTransferController {

    private final DspTransferService dspTransferService;

    private final DspTokenValidationService dspTokenValidationService;

    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    public DspTransferController(DspTransferService dspTransferService, DspTokenValidationService dspTokenValidationService) {
        this.dspTransferService = dspTransferService;
        this.dspTokenValidationService = dspTokenValidationService;
    }

    /**
     * Initiates a Pull Transfer Process.
     * This starts the process for the consumer to request data from the provider.
     *
     * @param requestBody - the request body of the incoming transfer message
     * @param authHeader  - the Authorization header value of the incoming message
     * @return - a response indicating the initiation status of the transfer process
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/request")
    public ResponseEntity<byte[]> createPullTransferProcess(@RequestBody String requestBody,
                                                            @RequestHeader("Authorization") String authHeader) {

        try {
            String partnerId = dspTokenValidationService.validateToken(authHeader);
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            ResponseRecord responseRecord = dspTransferService.handleNewTransfer(requestBody, partnerId);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/{providerPid}/completion")
    public ResponseEntity<byte[]> completeTransfer(@RequestBody String requestBody,
                                                   @RequestHeader("Authorization") String authString, @PathVariable("providerPid") UUID providerPid) {
        try {
            String partnerId = dspTokenValidationService.validateToken(authString);
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ResponseRecord responseRecord = dspTransferService.handleCompletionRequest(requestBody, partnerId, providerPid);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Endpoint for refreshing a token.
     *
     * @param grantType   - the type of grant
     * @param refreshToken - the refresh token
     * @param authToken  - the Authorization header value
     * @return - a response containing the new token
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/refresh")
    public ResponseEntity<byte[]> refreshToken(
            @RequestParam(name = "grant_type", required = false) String grantType,
            @RequestParam(name = "refresh_token", required = false) String refreshToken,
            @RequestHeader("Authorization") String authToken) {
        try {
            String partnerId = dspTokenValidationService.validateToken(authToken);
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (!GRANT_TYPE_REFRESH_TOKEN.equals(grantType)) {
                return ResponseEntity.badRequest().build();
            }

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            String audience = dspTokenValidationService.validateRefreshToken(refreshToken);
            if (audience == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            if (!partnerId.equals(audience)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            ResponseRecord response = dspTransferService.handleRefreshTokenRequest(refreshToken, authToken, partnerId);
            return ResponseEntity.status(response.statusCode()).body(response.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
