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
public class DefaultIdentityHubTokenProviderService extends BaseTokenProviderService {

    public DefaultIdentityHubTokenProviderService(RestClient restClient, Environment environment, EnvService envService) {
        super(restClient, environment, envService);
    }


    @Override
    public String getWrappedToken(String partnerDid, String tokenFromPartner) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_secret", initialSecret);
        requestBody.add("client_id", backendId);
        requestBody.add("audience", partnerDid);
        requestBody.add("token", tokenFromPartner);
        return obtainSelfSignedSignatureFromSTS(requestBody);
    }

    protected String provideTokenForPartner(String partnerDid) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_secret", initialSecret);
        requestBody.add("client_id", backendId);
        requestBody.add("audience", partnerDid);
        requestBody.add("bearer_access_scope", getPreparedScope());
        return obtainSelfSignedSignatureFromSTS(requestBody);
    }

    /**
     * Obtains a self-signed token from the STS server that was specified in the properties.
     * Key-value pairs can be inserted with the additionalKeyValuePairs list. Each two consecutive entries are
     * interpreted as key-value pairs, notable keys include "token" or "bearer_access_scope". Also note, that potentially
     * we could provide multiple values for one key (that's why a "List" instead of a "Map" is used here).
     *
     * @return the token from the STS
     */
    String obtainSelfSignedSignatureFromSTS(MultiValueMap<String, String> requestBody) {
        if (initialSecret == null) {
            String vaultResponse = restClient.get()
                    .uri(vaultSecretUrl)
                    .header("X-Vault-Token", vaultRootToken)
                    .retrieve()
                    .body(String.class);
            JsonObject vaultResponseJson = parse(vaultResponse);
            initialSecret = vaultResponseJson.getJsonObject("data").getJsonObject("data").getString("content");
            if (initialSecret != null) {
                log.info("STS Secret found");
            }
        }
        String stsResponse = restClient.post()
                .uri(stsUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.info("STS request status: " + res.getStatusCode());
                })
                .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
                    log.info("STS request status: " + res.getStatusCode());
                })
                .body(String.class);
        var stsResponseObject = JsonUtils.parse(stsResponse);
        return stsResponseObject.getString("access_token").strip();
    }
}
