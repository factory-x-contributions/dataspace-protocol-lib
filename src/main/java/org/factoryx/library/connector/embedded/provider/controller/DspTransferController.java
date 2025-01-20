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
import org.factoryx.library.connector.embedded.provider.model.ResponseRecord;
import org.factoryx.library.connector.embedded.provider.service.DspTransferService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Slf4j
public class DspTransferController {

    private final DspTransferService dspTransferService;

    public DspTransferController(DspTransferService dspTransferService) {
        this.dspTransferService = dspTransferService;
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
            var authJson = JsonUtils.parse(authHeader);
            String clientId = authJson.getString("clientId");
            String audience = authJson.getString("audience");
            ResponseRecord responseRecord = dspTransferService.handleNewTransfer(requestBody, clientId, audience);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/{providerPid}/completion")
    public ResponseEntity<byte[]> completeTransfer(@RequestBody String requestBody,
                                                   @RequestHeader("Authorization") String authString, @PathVariable("providerPid") UUID providerPid) {
        ResponseRecord responseRecord = dspTransferService.handleCompletionRequest(requestBody, authString,
                providerPid);
        return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
    }
}
