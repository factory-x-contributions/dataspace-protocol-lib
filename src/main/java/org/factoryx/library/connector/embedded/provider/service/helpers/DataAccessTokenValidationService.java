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

package org.factoryx.library.connector.embedded.provider.service.helpers;

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.service.AuthorizationService;
import org.factoryx.library.connector.embedded.provider.service.ContractRecordService;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

import static org.factoryx.library.connector.embedded.provider.service.AuthorizationService.CONTRACT_ID;
import static org.factoryx.library.connector.embedded.provider.service.AuthorizationService.DATA_ADDRESS;

@Service
@Slf4j
/**
 * This service can check whether a given token is valid for a given request URI
 * in the context of a DSP transfer request.
 *
 * @author eschrewe
 *
 */
public class DataAccessTokenValidationService {

    private final AuthorizationService authorizationService;
    private final ContractRecordService contractRecordService;
    private final String expectedIssuer;

    public DataAccessTokenValidationService(AuthorizationService authorizationService, ContractRecordService contractRecordService, EnvService envService) {
        this.authorizationService = authorizationService;
        this.contractRecordService = contractRecordService;
        this.expectedIssuer = envService.getSingleAssetReadOnlyDataAccessIssuer();
    }

    public boolean validateDataAccessTokenForAssetId(String token, String assetId) {
        try {
            Objects.requireNonNull(token, "Token must not be null");
            Objects.requireNonNull(assetId, "AssetId must not be null");
            token = token.replace("Bearer ", "").replace("bearer ", "");
            var claims = authorizationService.extractAllClaims(token);
            String contractId = claims.get(CONTRACT_ID).toString();
            String dataAddress = claims.get(DATA_ADDRESS).toString();
            String issuer = claims.getIssuer();
            NegotiationRecord negotiationRecord = contractRecordService.findByContractId(UUID.fromString(contractId));
            return negotiationRecord != null
                    && assetId.equals(negotiationRecord.getTargetAssetId())
                    && NegotiationState.FINALIZED.equals(negotiationRecord.getState())
                    && authorizationService.validateToken(token)
                    && dataAddress.endsWith(assetId)
                    && expectedIssuer.equals(issuer);
        } catch (Exception e) {
            log.error("Failure while validating token", e);
            return false;
        }
    }
}
