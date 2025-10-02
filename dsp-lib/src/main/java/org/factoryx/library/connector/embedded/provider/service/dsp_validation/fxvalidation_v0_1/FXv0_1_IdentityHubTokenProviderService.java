package org.factoryx.library.connector.embedded.provider.service.dsp_validation.fxvalidation_v0_1;

import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.parse;

@Service
@Slf4j
@ConditionalOnExpression("'${org.factoryx.library.validationservice:}'=='fxv0_1' and '${org.factoryx.library.validationservice.stsapi:}'=='identityhub'")
public class FXv0_1_IdentityHubTokenProviderService extends FXv0_1_AbstractTokenProviderService {

    private final EnvService envService;
    private final RestClient restClient;

    @Value("${org.factoryx.library.fxv01.vaultroottoken:root}")
    private String vaultRootToken;

    @Value("${org.factoryx.library.fxv01.vaulturl:http://provider-vault:8200}")
    private String vaultBaseUrl;

    @Value("${org.factoryx.library.fxv01.vaultsecretalias:did%3Aweb%3Aprovider-identityhub%253A7083%3Aprovider-sts-client-secret}")
    private String vaultSecretAlias;

    @Value("${org.factoryx.library.fxv01.identityhub.url:http://provider-sts-service:8082/api/sts/token}")
    private String identityHubTokenUrl;

    @Value("${org.factoryx.library.fxv01.bearer:false}")
    private boolean addBearer;

    /**
     * Is initialized at runtime via request to the vault
     */
    private String stsSecret;

    public FXv0_1_IdentityHubTokenProviderService(EnvService envService, RestClient restClient) {
        this.envService = envService;
        this.restClient = restClient;
    }

    @Override
    public String provideTokenForPartner(NegotiationRecord record) {
        return (addBearer ? "Bearer " : "") + provideTokenForPartner(record.getPartnerId());
    }

    @Override
    public String provideTokenForPartner(TransferRecord record) {
        return (addBearer ? "Bearer " : "") + provideTokenForPartner(record.getPartnerId());
    }


    @Override
    String getWrappedToken(String partnerDid, String tokenFromPartner) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_secret", stsSecret);
        requestBody.add("client_id", envService.getBackendId());
        requestBody.add("audience", partnerDid);
        requestBody.add("token", tokenFromPartner);
        return obtainSelfSignedSignatureFromSTS(requestBody);
    }



    private String provideTokenForPartner(String partnerDid) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_secret", stsSecret);
        requestBody.add("client_id", envService.getBackendId());
        requestBody.add("audience", partnerDid);
        requestBody.add("bearer_access_scope", "org.eclipse.edc.vc.type:MembershipCredential:read org.eclipse.edc.vc.type:DataProcessorCredential:read");
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
        String stsResponse = restClient.post()
                .uri(identityHubTokenUrl)
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
