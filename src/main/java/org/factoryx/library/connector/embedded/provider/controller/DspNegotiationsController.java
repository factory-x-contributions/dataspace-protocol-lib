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
import org.factoryx.library.connector.embedded.provider.service.DspNegotiationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Slf4j
/**
 * Endpoint for receiving DSP requests related to negotiations
 * @author eschrewe
 */
public class DspNegotiationsController {

    private final DspNegotiationService dspNegotiationService;
    private final DspTokenValidationService dspTokenValidationService;

    public DspNegotiationsController(DspNegotiationService dspNegotiationService, DspTokenValidationService dspTokenValidationService) {
        this.dspNegotiationService = dspNegotiationService;
        this.dspTokenValidationService = dspTokenValidationService;
    }

    /**
     * Endpoint of the DSP protocol for receiving new contract requests from a partner edc connector
     *
     * @param stringBody - the request body of the incoming message
     * @param authString - the Authorization header value of the incoming message
     * @return - a response ACK-body and code 201, if successful
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/negotiations/request")
    public ResponseEntity<byte[]> postNegotiationsNewRequestController(@RequestBody String stringBody,
                                                                       @RequestHeader("Authorization") String authString) {
        try {
            String partnerId = dspTokenValidationService.validateToken(authString);
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ResponseRecord responseRecord = dspNegotiationService.handleNewNegotiation(stringBody, partnerId);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }


    /**
     * Endpoint of the DSP protocol for receiving verification requests from a partner edc connector
     *
     * @param stringBody - the request body of the incoming message
     * @param authString - the Authorization header value of the incoming message
     * @param providerPid - the process id of the ongoing negotiation
     * @return - a response without body and code 200, if successful
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/negotiations/{providerPid}/agreement/verification")
    public ResponseEntity<byte[]> postNegotiationsVerificationController(@RequestBody String stringBody,
                                                                         @RequestHeader("Authorization") String authString,
                                                                         @PathVariable("providerPid") UUID providerPid) {
        try {
            String partnerId = dspTokenValidationService.validateToken(authString);
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized request".getBytes());
            }
            ResponseRecord responseRecord = dspNegotiationService.handleVerificationRequest(stringBody, partnerId, providerPid);
            return ResponseEntity.status(responseRecord.statusCode()).build();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}
