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

import static org.factoryx.library.connector.embedded.provider.service.AuthorizationService.*;

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
    private final String expectedReadOnlyIssuer;
    private final String expectedWriteAccessIssuer;

    public DataAccessTokenValidationService(AuthorizationService authorizationService, ContractRecordService contractRecordService, EnvService envService) {
        this.authorizationService = authorizationService;
        this.contractRecordService = contractRecordService;
        this.expectedReadOnlyIssuer = envService.getSingleAssetReadOnlyDataAccessIssuer();
        this.expectedWriteAccessIssuer = envService.getApiAssetWriteAccessIssuer();
    }

    public boolean validateWriteAccessTokenForAssetId(String token, String assetId) {
        try {
            Objects.requireNonNull(token, "Token must not be null");
            Objects.requireNonNull(assetId, "AssetId must not be null");
            token = token.replace("Bearer ", "").replace("bearer ", "");
            var claims = authorizationService.extractAllClaims(token);
            String contractId = claims.getStringClaim(CONTRACT_ID);
            String claimsAssetId = claims.getStringClaim(API_ASSET_ID);
            String claimsIssuer = claims.getIssuer();
            NegotiationRecord negotiationRecord = contractRecordService.findByContractId(UUID.fromString(contractId));
            boolean result = negotiationRecord != null
                    && assetId.equals(claimsAssetId)
                    && claimsIssuer.equals(expectedWriteAccessIssuer)
                    && NegotiationState.FINALIZED.equals(negotiationRecord.getState())
                    && authorizationService.validateToken(token);
            if (result) {
                log.info("Granted write access for partner {}", negotiationRecord.getPartnerId());
            }
            return result;
        } catch (Exception e) {
            log.error("Failure while validating token", e);
            return false;
        }
    }

    public boolean validateDataAccessTokenForAssetId(String token, String assetId) {
        try {
            Objects.requireNonNull(token, "Token must not be null");
            Objects.requireNonNull(assetId, "AssetId must not be null");
            token = token.replace("Bearer ", "").replace("bearer ", "");
            var claims = authorizationService.extractAllClaims(token);
            String contractId = claims.getStringClaim(CONTRACT_ID);
            String dataAddress = claims.getStringClaim(DATA_ADDRESS);
            String issuer = claims.getIssuer();
            NegotiationRecord negotiationRecord = contractRecordService.findByContractId(UUID.fromString(contractId));
            return negotiationRecord != null
                    && assetId.equals(negotiationRecord.getTargetAssetId())
                    && NegotiationState.FINALIZED.equals(negotiationRecord.getState())
                    && authorizationService.validateToken(token)
                    && dataAddress.endsWith(assetId)
                    && expectedReadOnlyIssuer.equals(issuer);
        } catch (Exception e) {
            log.error("Failure while validating token", e);
            return false;
        }
    }

    public boolean validateRefreshToken(String refreshToken, String partnerId) {
        try {
            Objects.requireNonNull(refreshToken, "Token must not be null");
            Objects.requireNonNull(partnerId, "PartnerId must not be null");
            
            refreshToken = refreshToken.replace("Bearer ", "").replace("bearer ", "");
            var refreshTokenClaims = authorizationService.extractAllClaims(refreshToken);
            String refreshTokenIssuer = refreshTokenClaims.getIssuer();
            String refreshTokenSubject = refreshTokenClaims.getSubject();
            String accessToken = refreshTokenClaims.getStringClaim(AuthorizationService.TOKEN);

            var accessTokenClaims = authorizationService.extractAllClaims(accessToken);
            String contractId = accessTokenClaims.getStringClaim(CONTRACT_ID);
            String dataAddress = accessTokenClaims.getStringClaim(DATA_ADDRESS);
            String accessTokenIssuer = accessTokenClaims.getIssuer();

            NegotiationRecord negotiationRecord = contractRecordService.findByContractId(UUID.fromString(contractId));

            return negotiationRecord != null
                    && NegotiationState.FINALIZED.equals(negotiationRecord.getState())
                    && expectedReadOnlyIssuer.equals(accessTokenIssuer)
                    && expectedReadOnlyIssuer.equals(refreshTokenIssuer)
                    && partnerId.equals(refreshTokenSubject)
                    && dataAddress.endsWith(negotiationRecord.getTargetAssetId())
                    && authorizationService.validateToken(refreshToken);
        } catch (Exception e) {
            log.error("Failure while validating refresh token", e);
            return false;
        }
    }
}
