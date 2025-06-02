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

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.parse;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.prettyPrint;

@Service
@Slf4j
@ConditionalOnProperty(name = "org.factoryx.library.validationservice", havingValue = "mvd")
public class MvdValidationService implements DspTokenValidationService {

    /**
     * Cache for Did-documents
     */
    private final HashMap<String, DidDocCacheEntry> didDocumentsCache = new HashMap<>();

    /**
     * Interval after which a renewed fetch must be performed
     */
    private final Duration didCacheUpdateInterval = Duration.ofDays(1);

    private record DidDocCacheEntry(JsonObject didDocument, LocalDateTime lastUpdated) {
    }

    /**
     * Cache for seen jti's. The attached value represents the timestamp, after which the
     * respective token has expired and thus became obsolete anyway. Expired tokens are expected
     * to be purged.
     */
    private final HashMap<String, Long> seenJtis = new HashMap<>();

    /**
     * Cache for token-signature-verifiers. The outer map's key is a partner's did.
     * The inner map's key is a key-id. The value contains a verifier that is expected
     * to verify all signatures that were created with that key.
     */
    private final HashMap<String, HashMap<String, JWSVerifier>> knownJwsVerifiers = new HashMap<>();

    @Value("${org.factoryx.library.mvd.trustedissuer:did:web:dataspace-issuer}")
    private String TRUSTED_ISSUER;

    private final RestClient restClient;
    private final EnvService envService;
    private final MvdTokenProviderService mvdTokenProviderService;

    public MvdValidationService(RestClient restClient, EnvService envService, MvdTokenProviderService mvdTokenProviderService) {
        this.restClient = restClient;
        this.envService = envService;
        this.mvdTokenProviderService = mvdTokenProviderService;
    }

    @Override
    public Map<String, String> validateToken(String token) {
        try {
            log.info("Incoming token: \n{}", token);
            SignedJWT jwt = SignedJWT.parse(token);
            var claims = jwt.getJWTClaimsSet();
            String partnerDid = claims.getStringClaim("sub");

            String accessTokenForPartnerCredentialService = claims.getStringClaim("token");
            log.info("Received AccessToken from partner:\n{}", accessTokenForPartnerCredentialService);
            SignedJWT at = SignedJWT.parse(accessTokenForPartnerCredentialService);
            log.info("Payload of Access Token: \n{}", prettyPrint(at.getPayload().toString()));
            boolean signatureCheckResult = verifyTokenSignature(jwt, partnerDid);
            boolean tokenBasicCheckResult = basicValidation(jwt);

            String selfSignedTokenForPartnerCredentialService = mvdTokenProviderService.obtainSelfSignedSignatureFromSTS(
                    partnerDid, List.of("token", accessTokenForPartnerCredentialService));

            boolean membershipCheck = checkMembershipVerifiablePresentation(selfSignedTokenForPartnerCredentialService, partnerDid);
            if (signatureCheckResult && tokenBasicCheckResult && membershipCheck) {
                return Map.of(ReservedKeys.partnerId.toString(), partnerDid,
                        ReservedKeys.credentials.toString(), "dataspacemember");
            }
            log.warn("Signature check: {}, membership check: {}, basic check: {}", signatureCheckResult, membershipCheck, tokenBasicCheckResult);
            return null;
        } catch (Exception e) {
            log.error("Failure while validating token {}", token, e);
            return null;
        }
    }

    /**
     * Resolves the key for the given partner and tries to verify the given token's signature with it
     *
     * @param token      the token, whose signature is to be checked
     * @param partnerDid the id of the partner, who is expected to be the signer of the token
     * @return true if verification was successful, false otherwise
     */
    private boolean verifyTokenSignature(SignedJWT token, String partnerDid) {
        log.info("Validating token signature for signer {}", partnerDid);
        try {
            HashMap<String, JWSVerifier> verifiers = knownJwsVerifiers.computeIfAbsent(partnerDid, any -> new HashMap<>());
            JWSVerifier jwsVerifier = verifiers.get(token.getHeader().getKeyID());
            if (jwsVerifier == null) {
                log.info("No cached verifier found for {}, retrying...", partnerDid);
                // invalidate dic-doc cache and try again
                didDocumentsCache.put(partnerDid, null);
                retrieveDidDocFromCacheOrFetch(partnerDid);

                jwsVerifier = verifiers.get(token.getHeader().getKeyID());
                log.info("Retry yielded result? {}", jwsVerifier != null);
            }
            boolean signatureCheck = token.verify(jwsVerifier);
            log.info("JWS Verifier result: {}", signatureCheck);
            return signatureCheck;
        } catch (Exception e) {
            log.error("Failure while validating token signature", e);
            return false;
        }
    }

    /**
     * Performs verification steps required by the DCP, that aren't done elsewhere.
     *
     * @param token the token to be validated
     * @return true, if all checks turned out as expected, false otherwise
     */
    private boolean basicValidation(SignedJWT token) {
        try {
            log.debug("Basic validation for \n{}", prettyPrint(token.getPayload().toString()));
            var claims = token.getJWTClaimsSet();
            boolean valid = claims.getStringClaim("iss").equals(claims.getStringClaim("sub"));
            log.debug("Valid after iss=sub {}", valid);
            valid = valid && claims.getAudience().contains(envService.getBackendId());
            log.debug("Valid after aud=myself {}", valid);
            long now = Instant.now().toEpochMilli();
            long leeway = 5000; // five seconds leeway
            long nbf = claims.getNotBeforeTime().getTime();
            valid = valid && nbf <= now + leeway;
            log.debug("Valid after nbf {}", valid);
            long exp = claims.getExpirationTime().getTime();
            valid = valid && exp >= now - leeway;
            log.debug("Valid after exp {}", valid);
            String jti = claims.getJWTID();
            valid = valid && jti != null && !seenJtis.containsKey(jti);
            log.debug("Valid after jti {}", valid);
            seenJtis.put(jti, exp + leeway);
            cleanUpSeenJtis();

            return valid;
        } catch (Exception e) {
            log.error("Failure while validating token", e);
        }
        return false;
    }

    /**
     * In order to stop the seenJtis map from growing infinitely large,
     * we purge all token-id's which would be rejected because of expiration
     * anyway.
     */
    private void cleanUpSeenJtis() {
        long now = Instant.now().toEpochMilli();
        HashSet<String> expiredJtis = new HashSet<>();
        for (var entry : seenJtis.entrySet()) {
            if (entry.getValue() < now) {
                expiredJtis.add(entry.getKey());
            }
        }
        expiredJtis.forEach(seenJtis::remove);
    }

    /**
     * Derives the url of the did-document from the given partnerDid and
     * attempts a GET request to obtain it.
     *
     * @param partnerDid the did of the partner in question
     * @return the did-document, if successful, otherwise null
     */
    private JsonObject fetchDidDoc(String partnerDid) {
        String url = partnerDid.replace("did:web:", "");
        url = url.replace(":", "/");
        url = url.replace("%3A", ":");
        url = envService.getURLPrefix() + url;
        URI uri = URI.create(url);
        uri = uri.getPath().isEmpty() ? uri.resolve("/.well-known/did.json") : URI.create(url + "/did.json");
        String didDocResponse = restClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.info("Status of Did-Doc Request for {}: {}", partnerDid, res.getStatusCode());
                })
                .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
                    log.info("Status of Did-Doc Request for {}: {}", partnerDid, res.getStatusCode());
                })
                .body(String.class);
        JsonObject didJson = parse(didDocResponse);
        if (didJson.getString("id").equals(partnerDid)) {
            return didJson;
        }
        return null;
    }

    /**
     * Returns the did-document for the partner with the given did, either by using a cached document if possible
     * or by triggering a http GET request, if necessary.
     *
     * @param partnerDid the did of the partner in question
     * @return the did-document, if successful, otherwise null
     */
    private JsonObject retrieveDidDocFromCacheOrFetch(String partnerDid) {
        try {
            DidDocCacheEntry didDocCacheEntry = didDocumentsCache.get(partnerDid);
            if (didDocCacheEntry != null && didDocCacheEntry.lastUpdated() != null && didDocCacheEntry.didDocument() != null) {
                if (LocalDateTime.now().isBefore(didDocCacheEntry.lastUpdated().plus(didCacheUpdateInterval))) {
                    return didDocCacheEntry.didDocument();
                }
            }
            JsonObject didJson = fetchDidDoc(partnerDid);
            if (didJson == null) {
                return null;
            }
            log.info("Retrieved did-doc:\n{}", prettyPrint(didJson));
            for (var entry : didJson.getJsonArray("verificationMethod")) {
                if (entry instanceof JsonObject entryObject) {
                    try {
                        String keyId = entryObject.getString("id");
                        if (keyId == null) {
                            log.error("Missing key id: \n{}", prettyPrint(entryObject));
                            continue;
                        }
                        JsonObject publicKeyJwk = entryObject.getJsonObject("publicKeyJwk");
                        JWK jwk = JWK.parse(publicKeyJwk.toString());
                        if (jwk.getKeyType().equals(KeyType.EC)) {
                            JWSVerifier jwsVerifier = new ECDSAVerifier(jwk.toPublicJWK().toECKey());
                            var verifierMap = knownJwsVerifiers.computeIfAbsent(partnerDid, any -> new HashMap<>());
                            verifierMap.put(keyId, jwsVerifier);
                        } else if (jwk.getKeyType().equals(KeyType.OKP)) {
                            JWSVerifier jwsVerifier = new Ed25519Verifier((OctetKeyPair) jwk.toPublicJWK());
                            var verifierMap = knownJwsVerifiers.computeIfAbsent(partnerDid, any -> new HashMap<>());
                            verifierMap.put(keyId, jwsVerifier);
                        } else {
                            log.error("Unsupported encryption algorithm {}", jwk.getKeyType());
                        }

                    } catch (Exception e) {
                        log.error("Failure while retrieving public key for {}", partnerDid, e);
                    }
                }
            }
            didDocumentsCache.put(partnerDid, new DidDocCacheEntry(didJson, LocalDateTime.now()));
            return didJson;
        } catch (Exception e) {
            log.error("Failure while retrieving Did-Doc Request {}", partnerDid, e);
            return null;
        }
    }

    /**
     * Retrieves and inspects the membership credential for a given partner, using the provided token.
     *
     * @param selfSignedTokenForPartnerCredentialService a self-signed token that wraps an access token from the partner
     * @param partnerDid                                 the id of the partner in question
     * @return true if the check was successful, false otherwise
     */
    private boolean checkMembershipVerifiablePresentation(String selfSignedTokenForPartnerCredentialService,
                                                          String partnerDid) {
        try {
            String credentialServiceUrl = extractCredentialServiceUrlFromDidDocument(retrieveDidDocFromCacheOrFetch(partnerDid));
            String credServiceResponse = restClient
                    .post()
                    .uri(credentialServiceUrl + "/presentations/query")
                    .header("Authorization", "Bearer " + selfSignedTokenForPartnerCredentialService)
                    .header("Content-Type", "application/json")
                    .body(getPresentationQuery())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.info("Call to CredentialService Endpoint Status: " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
                        log.info("Call to CredentialService Endpoint Status: " + res.getStatusCode());
                    })
                    .body(String.class);
            JsonObject credServiceResponseJson = parse(credServiceResponse);
            log.info("Got Response from CredentialService \n{}", prettyPrint(credServiceResponseJson));
            if (credServiceResponseJson.getString("type").equals("PresentationResponseMessage")) {
                String presentationToken = credServiceResponseJson.getString("presentation");
                SignedJWT signedPresentationJWT = SignedJWT.parse(presentationToken);
                boolean signatureValid = verifyTokenSignature(signedPresentationJWT, partnerDid);
                if (!signatureValid) {
                    log.info("Signature validation failed for presentation token: {}", presentationToken);
                    return false;
                }
                JsonObject payloadObject = parse(signedPresentationJWT.getPayload().toString());
                String verifiableCredentialToken = payloadObject.getJsonObject("vp").getJsonArray("verifiableCredential").getString(0);
                SignedJWT vcJWT = SignedJWT.parse(verifiableCredentialToken);
                String issuer = vcJWT.getJWTClaimsSet().getIssuer();
                log.info("Issuer of VC: {}", issuer);

                boolean vcSignatureValid = verifyTokenSignature(vcJWT, issuer);

                if (!vcSignatureValid || !TRUSTED_ISSUER.equals(issuer)) {
                    log.info("Signature validation failed for issuer of vc: {}", issuer);
                    return false;
                }

                JsonObject vcPayloadObject = parse(vcJWT.getPayload().toString());
                log.info("VC Payload: \n{}", prettyPrint(vcPayloadObject));
                JsonObject credentialSubject = vcPayloadObject.getJsonObject("vc").getJsonObject("credentialSubject");
                String subjectId = credentialSubject.getString("id");
                JsonObject membershipObject = credentialSubject.getJsonObject("membership");
                String membershipType = membershipObject.getString("membershipType");
                if (subjectId.equals(partnerDid) && "FullMember".equals(membershipType)) {
                    log.info("Membership successfully confirmed!");
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error while checking membership vpp", e);
            return false;
        }
        return false;
    }

    /**
     * A helper method that extracts the url of the credential service from a partner's did-document.
     *
     * @param didDocument the did-document, we want to extract the url from
     * @return the url, if successful, otherwise null
     */
    private String extractCredentialServiceUrlFromDidDocument(JsonObject didDocument) {
        try {
            var serviceArray = didDocument.getJsonArray("service");
            JsonObject credentialService = (JsonObject) serviceArray.stream()
                    .filter(entry -> entry instanceof JsonObject)
                    .filter(entry -> ((JsonObject) entry).getString("type").equals("CredentialService"))
                    .findFirst().orElse(null);
            if (credentialService != null) {
                String serviceEndpoint = credentialService.getString("serviceEndpoint");
                log.info("Credential Service url found {}", serviceEndpoint);
                return serviceEndpoint;
            } else {
                log.error("Credential Service not found");
                return null;
            }
        } catch (Exception e) {
            log.error("Failure while parsing Did-Doc \n{}", prettyPrint(didDocument), e);
            return null;
        }
    }


    /**
     * A helper method that generates a presentation query request body for a membership credential.
     *
     * @return the query request body
     */
    private String getPresentationQuery() {
        var presentationQuery = Json.createObjectBuilder();
        var context = Json.createArrayBuilder();
        context.add("https://identity.foundation/presentation-exchange/submission/v1")
                .add("https://w3id.org/tractusx-trust/v0.8");
        presentationQuery.add("@context", context.build());
        presentationQuery.add("@type", "PresentationQueryMessage");
        var scope = Json.createArrayBuilder();
        scope.add("org.eclipse.edc.vc.type:MembershipCredential:read");
        presentationQuery.add("scope", scope.build());
        return presentationQuery.build().toString();
    }

}
