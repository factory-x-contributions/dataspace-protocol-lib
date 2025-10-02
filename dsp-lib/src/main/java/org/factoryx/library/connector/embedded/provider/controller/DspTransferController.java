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
import org.factoryx.library.connector.embedded.provider.service.DspTransferService;
import org.factoryx.library.connector.embedded.provider.service.deserializers.DeserializerService;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.*;
import org.factoryx.library.connector.embedded.provider.service.helpers.DataAccessTokenValidationService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.createErrorResponse;

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
    private final DataAccessTokenValidationService dataAccessTokenValidationService;
    private final DeserializerService deserializerService;

    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    public DspTransferController(DspTransferService dspTransferService,
                                 DspTokenValidationService dspTokenValidationService,
                                 DataAccessTokenValidationService dataAccessTokenValidationService,
                                 DeserializerService deserializerService) {
        this.dspTransferService = dspTransferService;
        this.dspTokenValidationService = dspTokenValidationService;
        this.dataAccessTokenValidationService = dataAccessTokenValidationService;
        this.deserializerService = deserializerService;
    }

    /**
     * Initiates a Pull Transfer Process.
     * This starts the process for the consumer to request data from the provider.
     *
     * @param requestBody - the request body of the incoming transfer message
     * @param authString  - the Authorization header value of the incoming message
     * @return - a response indicating the initiation status of the transfer process
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/request")
    public ResponseEntity<byte[]> createPullTransferProcess_V_08(@RequestBody String requestBody,
                                                                 @RequestHeader("Authorization") String authString) {
        return handlePullTransferRequest(requestBody, authString, DspVersion.V_08);
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/transfers/request")
    public ResponseEntity<byte[]> createPullTransferProcess_V_2025_1(@RequestBody String requestBody,
                                                                     @RequestHeader("Authorization") String authString) {
        return handlePullTransferRequest(requestBody, authString, DspVersion.V_2025_1);
    }

    private ResponseEntity<byte[]> handlePullTransferRequest(String requestBody, String authString, DspVersion version) {
        log.info("transfers/request under version {}: \n{}", version, JsonUtils.prettyPrint(requestBody));
        try {
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            TransferRequestMessage transferRequestMessage = deserializerService.deserializeTransferRequestMessage(requestBody, version);
            if (transferRequestMessage == null) {
                return ResponseEntity.status(400).body(
                        createErrorResponse("unknown", "unknown",
                                "TransferError", List.of("Bad Request"), version));
            }
            ResponseRecord responseRecord = dspTransferService.handleNewTransfer(transferRequestMessage, partnerId, tokenValidationResult, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/{providerPid}/completion")
    public ResponseEntity<byte[]> transferCompletionEndpoint_V_08(@RequestBody String requestBody,
                                                                  @RequestHeader("Authorization") String authString, @PathVariable("providerPid") UUID providerPid) {
        return handleTransferCompletionMessage(requestBody, authString, providerPid, DspVersion.V_08);
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/transfers/{providerPid}/completion")
    public ResponseEntity<byte[]> transferCompletionEndpoint_V_2025_1(@RequestBody String requestBody,
                                                                      @RequestHeader("Authorization") String authString, @PathVariable("providerPid") UUID providerPid) {
        return handleTransferCompletionMessage(requestBody, authString, providerPid, DspVersion.V_2025_1);
    }

    private ResponseEntity<byte[]> handleTransferCompletionMessage(String requestBody, String authString, UUID providerPid, DspVersion version) {
        try {
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            TransferCompletionMessage transferCompletionMessage = deserializerService.deserializeTransferCompletionMessage(requestBody, version);
            if (transferCompletionMessage == null) {
                return ResponseEntity.status(400).body(
                        createErrorResponse(providerPid.toString(), "unknown",
                                "TransferError", List.of("Bad Request"), version));
            }
            ResponseRecord responseRecord = dspTransferService.handleCompletionRequest(transferCompletionMessage, partnerId, providerPid, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/{providerPid}")
    public ResponseEntity<byte[]> transferGetStatusEndpoint_V_08(@RequestHeader("Authorization") String authString,
                                                                 @PathVariable("providerPid") UUID providerPid) {
        return handleGetTransferRequest(authString, providerPid, DspVersion.V_08);
    }

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/transfers/{providerPid}")
    public ResponseEntity<byte[]> transferGetStatusEndpoint_V_2025_1(@RequestHeader("Authorization") String authString,
                                                                     @PathVariable("providerPid") UUID providerPid) {
        return handleGetTransferRequest(authString, providerPid, DspVersion.V_2025_1);
    }

    private ResponseEntity<byte[]> handleGetTransferRequest(String authString, UUID providerPid, DspVersion version) {
        try {
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ResponseRecord responseRecord = dspTransferService.handleGetStatusRequest(providerPid, partnerId, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/{providerPid}/termination")
    public ResponseEntity<byte[]> transferTerminationEndpoint_V_08(@RequestBody String requestBody,
                                                                   @RequestHeader("Authorization") String authString,
                                                                   @PathVariable("providerPid") UUID providerPid) {
        return handleTransferTerminationMessage(requestBody, authString, providerPid, DspVersion.V_08);
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/transfers/{providerPid}/termination")
    public ResponseEntity<byte[]> transferTerminationEndpoint_V_2025_1(@RequestBody String requestBody,
                                                                       @RequestHeader("Authorization") String authString,
                                                                       @PathVariable("providerPid") UUID providerPid) {
        return handleTransferTerminationMessage(requestBody, authString, providerPid, DspVersion.V_2025_1);
    }

    private ResponseEntity<byte[]> handleTransferTerminationMessage(String requestBody, String authString, UUID providerPid, DspVersion version) {
        try {
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            TransferTerminationMessage terminationMessage = deserializerService.deserializeTransferTerminationMessage(requestBody, version);
            if (terminationMessage == null) {
                return ResponseEntity.status(400).body(
                        createErrorResponse(providerPid.toString(), "unknown",
                                "TransferError", List.of("Bad Request"), version));
            }
            ResponseRecord responseRecord = dspTransferService.handleTerminationRequest(terminationMessage, partnerId, providerPid, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/{providerPid}/suspension")
    public ResponseEntity<byte[]> transferSuspensionEndpoint_V_08(@RequestBody String requestBody,
                                                                  @RequestHeader("Authorization") String authString,
                                                                  @PathVariable("providerPid") UUID providerPid) {
        return handleTransferSuspensionMessage(requestBody, authString, providerPid, DspVersion.V_08);
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/transfers/{providerPid}/suspension")
    public ResponseEntity<byte[]> transferSuspensionEndpoint_V_2025_1(@RequestBody String requestBody,
                                                                      @RequestHeader("Authorization") String authString,
                                                                      @PathVariable("providerPid") UUID providerPid) {
        return handleTransferSuspensionMessage(requestBody, authString, providerPid, DspVersion.V_2025_1);
    }

    private ResponseEntity<byte[]> handleTransferSuspensionMessage(String requestBody, String authString, UUID providerPid, DspVersion version) {
        try {
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            TransferSuspensionMessage suspensionMessage = deserializerService.deserializeTransferSuspensionMessage(requestBody, version);
            if (suspensionMessage == null) {
                return ResponseEntity.status(400).body(
                        createErrorResponse(providerPid.toString(), "unknown",
                                "TransferError", List.of("Bad Request"), version));
            }
            ResponseRecord responseRecord = dspTransferService.handleSuspensionRequest(suspensionMessage, partnerId, providerPid, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/{providerPid}/start")
    public ResponseEntity<byte[]> transferStartEndpoint_V_08(@RequestBody String requestBody,
                                                             @RequestHeader("Authorization") String authString,
                                                             @PathVariable("providerPid") UUID providerPid) {
        return handleTransferStartMessage(requestBody, authString, providerPid, DspVersion.V_08);
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/2025/1/transfers/{providerPid}/start")
    public ResponseEntity<byte[]> transferStartEndpoint_V_2025_1(@RequestBody String requestBody,
                                                                 @RequestHeader("Authorization") String authString,
                                                                 @PathVariable("providerPid") UUID providerPid) {
        return handleTransferStartMessage(requestBody, authString, providerPid, DspVersion.V_2025_1);
    }


    private ResponseEntity<byte[]> handleTransferStartMessage(String requestBody, String authString, UUID providerPid, DspVersion version) {
        try {
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            TransferStartMessage transferStartMessage = deserializerService.deserializeTransferStartMessage(requestBody, version);
            ResponseRecord responseRecord = dspTransferService.handleStartRequest(transferStartMessage, partnerId, providerPid, version);
            return ResponseEntity.status(responseRecord.statusCode()).body(responseRecord.responseBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint for refreshing a token.
     *
     * @param grantType    - the type of grant
     * @param refreshToken - the refresh token
     * @param authString   - the Authorization header value
     * @return - a response containing the new token
     */
    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/transfers/refresh")
    public ResponseEntity<byte[]> refreshToken(
            @RequestParam(name = "grant_type", required = false) String grantType,
            @RequestParam(name = "refresh_token", required = false) String refreshToken,
            @RequestHeader("Authorization") String authString) {
        try {
            Map<String, String> tokenValidationResult = dspTokenValidationService.validateToken(authString);
            String partnerId = tokenValidationResult.get(DspTokenValidationService.ReservedKeys.partnerId.toString());
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            if (!GRANT_TYPE_REFRESH_TOKEN.equals(grantType)) {
                log.warn("Invalid grant type: {}", grantType);
                return ResponseEntity.badRequest().build();
            }

            boolean refreshTokenValid = dataAccessTokenValidationService.validateRefreshToken(refreshToken, partnerId);
            if (!refreshTokenValid) {
                log.warn("Invalid refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            ResponseRecord response = dspTransferService.handleRefreshTokenRequest(refreshToken);

            log.info("Response: {}", response.responseBody());
            return ResponseEntity.status(response.statusCode()).body(response.responseBody());
        } catch (Exception e) {
            log.error("Error processing refresh token request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
