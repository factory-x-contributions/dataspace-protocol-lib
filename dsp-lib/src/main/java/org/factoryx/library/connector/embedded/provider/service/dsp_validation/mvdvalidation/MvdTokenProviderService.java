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

package org.factoryx.library.connector.embedded.provider.service.dsp_validation.mvdvalidation;

import jakarta.json.JsonObject;
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

@Service
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.validationservice", havingValue = "mvd")
public class MvdTokenProviderService implements DspTokenProviderService {

    private final EnvService envService;
    private final RestClient restClient;

    @Value("${org.factoryx.library.mvd.vaultroottoken:root}")
    private String vaultRootToken;

    @Value("${org.factoryx.library.mvd.vaulturl:http://provider-vault:8200}")
    private String vaultBaseUrl;

    @Value("${org.factoryx.library.mvd.vaultsecretalias:did%3Aweb%3Aprovider-identityhub%253A7083%3Aprovider-sts-client-secret}")
    private String vaultSecretAlias;

    @Value("${org.factoryx.library.mvd.ststokenurl:http://provider-sts-service:8082/api/sts/token}")
    private String stsTokenUrl;

    /**
     * Is initialized at runtime via request to the vault
     */
    private String stsSecret;

    public MvdTokenProviderService(EnvService envService, RestClient restClient) {
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
        return obtainSelfSignedSignatureFromSTS(partnerDid,
                List.of("bearer_access_scope",
                        "org.eclipse.edc.vc.type:MembershipCredential:read org.eclipse.edc.vc.type:DataProcessorCredential:read"));
    }

    /**
     * Obtains a self-signed token from the STS server that was specified in the properties.
     * Key-value pairs can be inserted with the additionalKeyValuePairs list. Each two consecutive entries are
     * interpreted as key-value pairs, notable keys include "token" or "bearer_access_scope". Also note, that potentially
     * we could provide multiple values for one key (that's why a "List" instead of a "Map" is used here).
     *
     * @param audience                the audience of the token
     * @param additionalKeyValuePairs each two consecutive items are interpreted as key-value pairs
     * @return the token from the STS
     */
    String obtainSelfSignedSignatureFromSTS(String audience, List<String> additionalKeyValuePairs) {
        if (stsSecret == null) {
            String vaultResponse = restClient.get()
                    .uri(vaultBaseUrl + "/v1/secret/data/" + vaultSecretAlias)
                    .header("X-Vault-Token", vaultRootToken)
                    .retrieve()
                    .body(String.class);
            JsonObject vaultResponseJson = parse(vaultResponse);
            stsSecret = vaultResponseJson.getJsonObject("data").getJsonObject("data").getString("content");
            if (stsSecret != null) {
                log.info("STS Secret found");
            }
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_secret", stsSecret);
        form.add("client_id", envService.getBackendId());
        form.add("audience", audience);
        for (int i = 0; i < additionalKeyValuePairs.size(); i += 2) {
            form.add(additionalKeyValuePairs.get(i), additionalKeyValuePairs.get(i + 1));
        }

        String stsResponse = restClient.post()
                .uri(stsTokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.info("STS request status: " + res.getStatusCode());
                })
                .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
                    log.info("STS request status: " + res.getStatusCode());
                })
                .body(String.class);
        var stsResponseObject = JsonUtils.parse(stsResponse);
        return "Bearer " + stsResponseObject.getString("access_token");
    }
}
