/*
 * Copyright (c) 2026. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.factoryx.library.connector.embedded.provider.service.dsp_validation.base;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.parse;

@Slf4j
public class DefaultDimWalletTokenProviderService extends BaseTokenProviderService {

    private String dimTokenUrl;
    private String dimClientId;

    public DefaultDimWalletTokenProviderService(RestClient restClient, Environment environment, EnvService envService) {
        super(restClient, environment, envService);
        dimTokenUrl = environment.getProperty("org.factoryx.library.dcpvalidation.dimtokenurl", "http://my-dim-token-url");
        dimClientId = environment.getProperty("org.factoryx.library.dcpvalidation.dimclientid", "my-client-id");
    }

    @Override
    protected String provideTokenForPartner(String partnerId) {
        JsonObject payload = Json.createObjectBuilder()
                .add("grantAccess", Json.createObjectBuilder()
                        .add("scope", "read")
                        .add("credentialTypes", Json.createArrayBuilder()
                                .add("VerifiableCredential")
                                .add("MembershipCredential"))
                        .add("consumerDid", backendId)
                        .add("providerDid", partnerId)
                        .build()).build();
        return obtainSelfSignedSignatureFromSTS(payload.toString());
    }

    public String getWrappedToken(String partnerDid, String tokenFromPartner) {
        JsonObject payload = Json.createObjectBuilder()
                .add("signToken", Json.createObjectBuilder()
                        .add("issuer", backendId)
                        .add("subject", backendId)
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
        String dimResponse = restClient.post()
                .uri(stsUrl)
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
        var stsResponseObject = JsonUtils.parse(dimResponse);
        return stsResponseObject.getString("jwt");
    }

    private String obtainDimAccessToken() {
        if (initialSecret == null) {
            String vaultResponse = restClient.get()
                    .uri(vaultSecretUrl)
                    .header("X-Vault-Token", vaultRootToken)
                    .retrieve()
                    .body(String.class);
            JsonObject vaultResponseJson = parse(vaultResponse);
            initialSecret = vaultResponseJson.getJsonObject("data").getJsonObject("data").getString("content");
            if (initialSecret != null) {
                initialSecret = initialSecret.strip();
                log.info("dimTokenAccessSecret found");
            }
        }
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_id", dimClientId);
        requestBody.add("client_secret", initialSecret);
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
