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
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.model.ResponseRecord;
import org.factoryx.library.connector.embedded.provider.service.DspNegotiationService;
import org.factoryx.library.connector.embedded.provider.service.deserializers.DeserializerService;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.ContractVerificationMessage;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.NegotiationTerminationMessage;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.ContractRequestMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.createErrorResponse;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.prettyPrint;

@RestController
@Slf4j
/**
 * Endpoint for receiving DSP requests related to negotiations
 * @author eschrewe
 */
public class DspNegotiationsController {

    private final DspNegotiationService dspNegotiationService;
    private final DeserializerService deserializerService;
    private final DspTokenValidationService dspTokenValidationService;

    public DspNegotiationsController(DspNegotiationService dspNegotiationService, DeserializerService deserializerService,
                                     DspTokenValidationService dspTokenValidationService) {
        this.dspNegotiationService = dspNegotiationService;
        this.deserializerService = deserializerService;
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
    public ResponseEntity<byte[]> postNegotiationsNewRequestControllerV_08(@RequestBody String stringBody,
                                                                       @RequestHeader("Authorization") String authString) {
        return handleNegotiationRequest(stringBody, authString, DspVersion.V_08);
    }

    /**
     * Endpoint of the DSP protocol for receiving new contract requests from a partner edc connector
     *
     * @param stringBody - the request body of the incoming message
     * @param authString - the Authorization header value of the incoming message
     * @return - a response ACK-body and code 201, if successful
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/negotiations/request")
    public ResponseEntity<byte[]> postNegotiationsNewRequestControllerV_2025_1(@RequestBody String stringBody,
                                                                               @RequestHeader("Authorization") String authString) {
        return handleNegotiationRequest(stringBody, authString, DspVersion.V_2025_1);
    }


    private ResponseEntity<byte[]> handleNegotiationRequest(String rawJson,  String authString, DspVersion version) {
        log.info("negotiations/request on version{}: \n{}", version, JsonUtils.prettyPrint(rawJson));
        log.info("Raw body \n{}", prettyPrint(rawJson));
        try {
            log.info("Starting validation");
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            log.info("Got Validation result: {}", tokenValidationResult);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            log.info("Starting Deserialization");
            ContractRequestMessage contractRequestMessage =
                    deserializerService.deserializeContractRequestMessage(rawJson, version);
            if (contractRequestMessage == null) {
                return ResponseEntity.status(400).body(
                        createErrorResponse("unknown", "unknown",
                                "ContractNegotiationError", List.of("Bad Request"), version));
            }
            ResponseRecord responseRecord =
                    dspNegotiationService.handleNewNegotiation(contractRequestMessage, partnerId, tokenValidationResult, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
    public ResponseEntity<byte[]> postNegotiationsVerificationControllerV_08(@RequestBody String stringBody,
                                                                         @RequestHeader("Authorization") String authString,
                                                                         @PathVariable("providerPid") UUID providerPid) {
        return handleVerificationRequest(stringBody, authString, providerPid, DspVersion.V_08);
    }

    /**
     * Endpoint of the DSP protocol for receiving verification requests from a partner edc connector
     *
     * @param stringBody - the request body of the incoming message
     * @param authString - the Authorization header value of the incoming message
     * @param providerPid - the process id of the ongoing negotiation
     * @return - a response without body and code 200, if successful
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/negotiations/{providerPid}/agreement/verification")
    public ResponseEntity<byte[]> postNegotiationsVerificationControllerV_2025_1(@RequestBody String stringBody,
                                                                                 @RequestHeader("Authorization") String authString,
                                                                                 @PathVariable("providerPid") UUID providerPid) {
        return handleVerificationRequest(stringBody, authString, providerPid, DspVersion.V_2025_1);
    }

    private ResponseEntity<byte[]> handleVerificationRequest(String rawJson, String authString, UUID providerPid, DspVersion version) {
        log.info("negotiations/agreement/verification on version {} \n{}", version, JsonUtils.prettyPrint(rawJson));
        try {
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized request".getBytes());
            }
            ContractVerificationMessage contractVerificationMessage =
                    deserializerService.deserializeContractVerificationMessage(rawJson, version);
            if (contractVerificationMessage == null) {
                return ResponseEntity.status(400).body(
                        createErrorResponse(providerPid.toString(), "unknown",
                                "ContractNegotiationError", List.of("Bad Request"), version));
            }
            ResponseRecord responseRecord =
                    dspNegotiationService.handleVerificationRequest(contractVerificationMessage, partnerId, providerPid, version);
            return ResponseEntity.status(responseRecord.statusCode()).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/negotiations/{providerPid}/termination")
    public ResponseEntity<byte[]> postNegotiationsTerminationControllerV_08(@RequestBody String stringBody,
                                                                            @RequestHeader("Authorization") String authString,
                                                                            @PathVariable("providerPid") UUID providerPid) {
        return handleTerminationRequest(stringBody, authString, providerPid, DspVersion.V_08);
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/negotiations/{providerPid}/termination")
    public ResponseEntity<byte[]> postNegotiationsTerminationControllerV_2025_1(@RequestBody String stringBody,
                                                                                @RequestHeader("Authorization") String authString,
                                                                                @PathVariable("providerPid") UUID providerPid) {
        return handleTerminationRequest(stringBody, authString, providerPid, DspVersion.V_2025_1);
    }

    private ResponseEntity<byte[]> handleTerminationRequest(String rawJson, String authString, UUID providerPid, DspVersion version) {
        try {
            log.info("negotiation/termination received under version {} \n{}", version, JsonUtils.prettyPrint(rawJson));
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized request".getBytes());
            }
            NegotiationTerminationMessage terminationMessage = deserializerService.deserializeNegotiationTerminationMessage(rawJson, version);
            if (terminationMessage == null) {
                return ResponseEntity.status(400).body(
                        createErrorResponse(providerPid.toString(), "unknown",
                                "ContractNegotiationError", List.of("Bad Request"), version));
            }

            ResponseRecord responseRecord = dspNegotiationService.handleNegotiationTerminationRequest(terminationMessage, partnerId, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/negotiations/{providerPid}")
    public ResponseEntity<byte[]> getNegotiationStatusV_08(@RequestHeader("Authorization") String authString,
                                                           @PathVariable("providerPid") UUID providerPid) {
        return handleGetNegotiationStatusRequest(authString, providerPid, DspVersion.V_08);
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/negotiations/{providerPid}")
    public ResponseEntity<byte[]> getNegotiationStatusV_2025_1(@RequestHeader("Authorization") String authString,
                                                               @PathVariable("providerPid") UUID providerPid) {
        return handleGetNegotiationStatusRequest(authString, providerPid, DspVersion.V_2025_1);
    }

    private ResponseEntity<byte[]> handleGetNegotiationStatusRequest(String authString, UUID providerPid, DspVersion version) {
        try {
            log.info("Negotiation status received request for {} under version {}", providerPid, version);
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized request".getBytes());
            }
            ResponseRecord responseRecord = dspNegotiationService.handleGetNegotiationStatusRequest(providerPid, partnerId, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(401).build();
        }
    }
}
