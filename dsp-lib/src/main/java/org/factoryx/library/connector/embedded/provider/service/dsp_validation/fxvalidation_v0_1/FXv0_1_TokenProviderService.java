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

package org.factoryx.library.connector.embedded.provider.service.dsp_validation.fxvalidation_v0_1;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.parse;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.prettyPrint;

@Service
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.validationservice", havingValue = "fxv0_1")
public class FXv0_1_TokenProviderService implements DspTokenProviderService {

    private final EnvService envService;
    private final RestClient restClient;

    @Value("${org.factoryx.library.fxv01.vaultroottoken:root}")
    private String vaultRootToken;

    @Value("${org.factoryx.library.fxv01.vaulturl:http://provider-vault:8200}")
    private String vaultBaseUrl;

    @Value("${org.factoryx.library.fxv01.vaultsecretalias:did%3Aweb%3Aprovider-identityhub%253A7083%3Aprovider-sts-client-secret}")
    private String vaultSecretAlias;

    @Value("${org.factoryx.library.fxv01.dimtokenurl:http://my-dim-token-url}")
    private String dimTokenUrl;

    @Value("${org.factoryx.library.fxv01.dimclientid:my-client-id}")
    private String dimClientId;

    @Value("${org.factoryx.library.fxv01.dimurl:http://my-dim-url}")
    private String dimUrl;

    /**
     * Is initialized at runtime via request to the vault
     */
    private String dimTokenAccessSecret;

    public FXv0_1_TokenProviderService(EnvService envService, RestClient restClient) {
        this.envService = envService;
        this.restClient = restClient;
    }

    @Override
    public String provideTokenForPartner(NegotiationRecord record) {
        return provideTokenForPartner(record.getPartnerId());
    }

    @Override
    public String provideTokenForPartner(TransferRecord record) {
        return provideTokenForPartner(record.getPartnerId());
    }

    private String provideTokenForPartner(String partnerDid) {

        JsonObject payload = Json.createObjectBuilder()
                .add("grantAccess", Json.createObjectBuilder()
                        .add("scope", "read")
                        .add("credentialTypes", Json.createArrayBuilder()
                                .add("VerifiableCredential")
                                .add("MembershipCredential"))
                        .add("consumerDid", envService.getBackendId())
                        .add("providerDid", partnerDid)
                        .build()).build();
        return obtainSelfSignedSignatureFromSTS(payload.toString());
    }

    String getWrappedToken(String partnerDid, String tokenFromPartner) {
        JsonObject payload = Json.createObjectBuilder()
                .add("signToken", Json.createObjectBuilder()
                    .add("issuer", envService.getBackendId())
                    .add("subject", envService.getBackendId())
                    .add("audience", partnerDid)
                    .add("token", tokenFromPartner)
                    .build()).build();
        return obtainSelfSignedSignatureFromSTS(payload.toString());
    }

    /**
     * Obtains a self-signed token from the STS server that was specified in the properties.
     * Key-value pairs can be inserted with the additionalKeyValuePairs list. Each two consecutive entries are
     * interpreted as key-value pairs, notable keys include "token" or "bearer_access_scope". Also note, that potentially
     * we could provide multiple values for one key (that's why a "List" instead of a "Map" is used here).
     *
     * @param payload                 the payload for the token service
     * @return the token from the STS
     */
    private String obtainSelfSignedSignatureFromSTS(String payload) {
        String dimCurrentToken = obtainDimAccessToken();
        log.info("Sending \n{}", prettyPrint(payload));
        String dimResponse = restClient.post()
                .uri(dimUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + dimCurrentToken)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.info("dim request status: " + res.getStatusCode());
                })
                .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
                    log.info("dim request status: " + res.getStatusCode());
                })
                .body(String.class);
        log.info("dim response: \n{}", dimResponse);
        var stsResponseObject = JsonUtils.parse(dimResponse);
        return stsResponseObject.getString("jwt");
    }

    String obtainDimAccessToken() {
        if (dimTokenAccessSecret == null) {
            String vaultRequestUrl = vaultBaseUrl + "/v1/secret/data/" + vaultSecretAlias;
            String vaultResponse = restClient.get()
                    .uri(vaultRequestUrl)
                    .header("X-Vault-Token", vaultRootToken)
                    .retrieve()
                    .body(String.class);
            JsonObject vaultResponseJson = parse(vaultResponse);
            dimTokenAccessSecret = vaultResponseJson.getJsonObject("data").getJsonObject("data").getString("content");
            if (dimTokenAccessSecret != null) {
                dimTokenAccessSecret = dimTokenAccessSecret.strip();
                log.info("dimTokenAccessSecret found");
            }
        }
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_id", dimClientId);
        requestBody.add("client_secret", dimTokenAccessSecret);
        String dimTokenResponse = restClient.post()
                .uri(dimTokenUrl)
                .body(requestBody)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.info("dim token request status: {}", res.getStatusCode());
                })
                .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
                    log.info("dim token request status: {}", res.getStatusCode());
                })
                .body(String.class);
        log.info("dim token response body: \n{}", dimTokenResponse);
        JsonObject jsonResponseBody = parse(dimTokenResponse);
        return jsonResponseBody.getString("access_token").strip();
    }
}
