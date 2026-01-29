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

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.Nonnull;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.parse;
import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.prettyPrint;

/**
 * This is an abstract class that can be extended to fit the needs of a specific dataspace
 * and/or deployment situation. The extending class must provide an implementation of the
 * {@link #evaluateVerifiablePresentations} method, see the docs there for more details.
 * <p>
 * Note that this class has a dependency on a subtype of {@link BaseTokenProviderService}.
 * You may choose to instantiate any of the {@link DefaultIdentityHubTokenProviderService}
 * or {@link DefaultDimWalletTokenProviderService} classes as beans in your Spring application context.
 * Or you could create your own subclass of the {@link BaseTokenProviderService}, if needed.
 */
@Slf4j
public abstract class BaseDcpValidationService implements DspTokenValidationService {

    /**
     * Cache for Did-documents
     */
    private final HashMap<String, DidDocCacheEntry> didDocumentsCache = new HashMap<>();

    /**
     * Interval after which a cached value must be renewed.
     */
    private final Duration cacheUpdateInterval;

    private interface CacheEntryWithUpdatedInstant {
        Instant lastUpdated();
    }

    private record DidDocCacheEntry(JsonObject didDocument,
                                    Instant lastUpdated) implements CacheEntryWithUpdatedInstant {
    }

    /**
     * Cache for BitStringStatusLists. The key is the URL, under which the StatusList was obtained.
     */
    private final HashMap<String, RevocationCacheEntry> revocationCache = new HashMap<>();

    private record RevocationCacheEntry(byte[] bitstringList,
                                        Instant lastUpdated) implements CacheEntryWithUpdatedInstant {
    }

    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

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
    protected final Set<String> TRUSTED_ISSUERS;
    private final boolean iamUseHttps;
    private final JsonArray presentationRequestContext;

    private final RestClient restClient;
    private final EnvService envService;
    private final BaseTokenProviderService tokenProviderService;
    private final BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();

    /**
     * Constructor for the BaseDcpValidationService, which allows for introducing a customized presentation
     * request context array.
     *
     * @param restClient                 Should be injected from the default bean by application context.
     * @param envService                 Should be injected from the default bean by application context.
     * @param baseTokenProviderService   Should be injected from the default bean by application context.
     * @param environment                Should be injected from the default bean by application context.
     * @param presentationRequestContext A customized presentation request context array.
     */
    protected BaseDcpValidationService(RestClient restClient, EnvService envService,
                                       BaseTokenProviderService baseTokenProviderService,
                                       Environment environment, JsonArray presentationRequestContext) {
        this.restClient = restClient;
        this.envService = envService;
        this.tokenProviderService = baseTokenProviderService;
        this.presentationRequestContext = presentationRequestContext;
        Security.addProvider(bouncyCastleProvider);
        iamUseHttps = environment.getProperty("org.factoryx.library.dcpvalidation.https", Boolean.class, true);
        String issuerValue = environment.getProperty("org.factoryx.library.dcpvalidation.trustedissuers", "");
        TRUSTED_ISSUERS = Arrays.stream(issuerValue.replace(" ", "").split(",")).collect(Collectors.toSet());
        log.info("Trusted issuers: {}", TRUSTED_ISSUERS);
        this.cacheUpdateInterval =  environment.getProperty("org.factoryx.library.dcpvalidation.cacheupdate.interval", Duration.class, Duration.ofHours(24));
    }

    /**
     * Subclasses may want to target this constructor, if they don't want to specify their own presentation
     * request context array and use the current default.
     *
     * @param restClient               Should be injected from the default bean by application context.
     * @param envService               Should be injected from the default bean by application context.
     * @param baseTokenProviderService Should be injected from the default bean by application context.
     * @param environment              Should be injected from the default bean by application context.
     */
    protected BaseDcpValidationService(RestClient restClient, EnvService envService,
                                       BaseTokenProviderService baseTokenProviderService,
                                       Environment environment) {
        this(restClient, envService, baseTokenProviderService, environment,
                Json.createArrayBuilder()
                        .add("https://w3id.org/tractusx-trust/v0.8")
                        .add("https://identity.foundation/presentation-exchange/submission/v1")
                        .build());
    }

    @Override
    public Map<String, String> validateToken(String token) {
        cacheLock.readLock().lock();
        try {
            if ("Bearer ".equalsIgnoreCase(token.substring(0, 7))) {
                token = token.substring(7);
            }
            SignedJWT jwt = SignedJWT.parse(token);
            var claims = jwt.getJWTClaimsSet();
            String partnerDid = claims.getStringClaim("sub");

            String accessTokenForPartnerCredentialService = claims.getStringClaim("token");
            boolean signatureCheckResult = verifyTokenSignature(jwt, partnerDid);
            boolean tokenBasicCheckResult = basicValidation(jwt);
            if (!signatureCheckResult || !tokenBasicCheckResult) {
                log.warn("Signature check: {}, basic check: {}", signatureCheckResult, tokenBasicCheckResult);
                return Map.of();
            }

            // get verifiable presentation(s) from partner credential service
            List<SignedJWT> verifiablePresentations = fetchVerifiablePresentations(accessTokenForPartnerCredentialService, partnerDid);

            // the implementation of the evaluateVerifiablePresentations - method is responsible for inspecting the
            // details of the vp's and decide, which properties it wants to assign to the partner in regard to the
            // concrete vc's he has shown.
            Map<String, String> output = evaluateVerifiablePresentations(verifiablePresentations, partnerDid);
            try {
                // can fail, if implementation of evaluateVerifiablePresentations returns immutable map.
                output.put(ReservedKeys.partnerId.toString(), partnerDid);
            } catch (Exception e) {
                output = new HashMap<>(output);
                output.put(ReservedKeys.partnerId.toString(), partnerDid);
            }

            return output;
        } catch (Exception e) {
            log.error("Failure while validating token {}", token, e);
            return Map.of();
        } finally {
            cacheLock.readLock().unlock();
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
                didDocumentsCache.put(partnerDid, null);
                retrieveDidDocFromCacheOrFetch(partnerDid);
                jwsVerifier = verifiers.get(token.getHeader().getKeyID());
            }
            boolean signatureCheck = token.verify(jwsVerifier);
            log.info("JWS Verifier result with Key-Id: {}, {}", token.getHeader().getKeyID(), signatureCheck);
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
            log.debug("Basic validation for \n{}", token.getPayload());
            var claims = token.getJWTClaimsSet();
            boolean valid = claims.getStringClaim("iss").equals(claims.getStringClaim("sub"));
            log.debug("Valid after iss=sub {}", valid);
            valid = valid && claims.getAudience().contains(envService.getBackendId());
            log.debug("Valid after aud=myself {}", valid);
            long now = Instant.now().toEpochMilli();
            long leeway = 5000; // five seconds leeway
            var nbfDate = claims.getNotBeforeTime();
            if (nbfDate != null) {
                long nbf = claims.getNotBeforeTime().getTime();
                valid = valid && nbf <= now + leeway;
            }
            log.debug("Valid after nbf {}", valid);

            long exp = claims.getExpirationTime().getTime();
            valid = valid && exp >= now - leeway;
            log.debug("Valid after exp {}", valid);
            String jti = claims.getJWTID();
            valid = valid && jti != null && !seenJtis.containsKey(jti);
            log.debug("Valid after jti {}", valid);
            seenJtis.put(jti, exp + leeway);
            return valid;
        } catch (Exception e) {
            log.error("Failure while validating token", e);
        }
        return false;
    }

    /**
     * Derives the url of the did-document from the given partnerDid and
     * attempts a GET request to obtain it.
     *
     * @param partnerDid the did of the partner in question
     * @return the did-document, if successful, otherwise null
     */
    private JsonObject fetchDidDoc(String partnerDid) {
        if (partnerDid == null || !partnerDid.startsWith("did:web:")) {
            log.warn("Invalid DID: {}", partnerDid);
            return null;
        }

        String remainder = partnerDid.substring("did:web:".length());
        String[] parts = remainder.split(":");
        String host = parts[0];
        String path = parts.length > 1 ? "/" + String.join("/", Arrays.copyOfRange(parts, 1, parts.length)) : "";
        String scheme = this.iamUseHttps ? "https" : "http";

        URI uri = URI.create(scheme + "://" + host + (path.isEmpty() ? "/.well-known/did.json" : path + "/did.json"));

        // handle redirects
        ResponseEntity<String> resp = this.restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(String.class);

        if (resp.getStatusCode().is3xxRedirection() && resp.getHeaders().getLocation() != null) {
            URI loc = resp.getHeaders().getLocation();
            URI follow = loc.isAbsolute() ? loc : uri.resolve(loc);
            log.info("Following redirect {} -> {}", uri, follow);
            resp = this.restClient.get().uri(follow).retrieve().toEntity(String.class);
        }

        if (!resp.getStatusCode().is2xxSuccessful()) {
            log.warn("DID-Doc Request for {} failed: {}", partnerDid, resp.getStatusCode());
            return null;
        }

        String didDocResponse = resp.getBody();
        JsonObject didJson = JsonUtils.parse(didDocResponse);
        return partnerDid.equals(didJson.getString("id")) ? didJson : null;
    }

    /**
     * Returns the did-document for the partner with the given did, either by using a cached document if possible
     * or by triggering an http GET request, if necessary.
     *
     * @param partnerDid the did of the partner in question
     * @return the did-document, if successful, otherwise null
     */
    private JsonObject retrieveDidDocFromCacheOrFetch(String partnerDid) {
        try {
            DidDocCacheEntry didDocCacheEntry = didDocumentsCache.get(partnerDid);
            if (didDocCacheEntry != null && didDocCacheEntry.lastUpdated() != null && didDocCacheEntry.didDocument() != null) {
                return didDocCacheEntry.didDocument();
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
                        log.info("Found public key: {}", jwk);
                        var verifierMap = knownJwsVerifiers.computeIfAbsent(partnerDid, any -> new HashMap<>());
                        if (jwk.getKeyType().equals(KeyType.EC)) {
                            ECKey ecKey = jwk.toPublicJWK().toECKey();
                            ECDSAVerifier jwsVerifier = new ECDSAVerifier(ecKey);
                            if ("secp256k1".equals(ecKey.getCurve().getName())) {
                                jwsVerifier.getJCAContext().setProvider(bouncyCastleProvider);
                            }
                            verifierMap.put(keyId, jwsVerifier);
                        } else if (jwk.getKeyType().equals(KeyType.OKP)) {
                            JWSVerifier jwsVerifier = new Ed25519Verifier(jwk.toOctetKeyPair());
                            verifierMap.put(keyId, jwsVerifier);
                        } else if (jwk.getKeyType().equals(KeyType.RSA)) {
                            RSAKey rsaKey = jwk.toRSAKey();
                            RSASSAVerifier verifier = new RSASSAVerifier(rsaKey);
                            verifier.getJCAContext().setProvider(bouncyCastleProvider);
                            verifierMap.put(keyId, verifier);
                        } else {
                            log.error("Unsupported encryption algorithm {}", jwk.getKeyType());
                        }

                    } catch (Exception e) {
                        log.error("Failure while retrieving public key for {}", partnerDid, e);
                    }
                }
            }
            didDocumentsCache.put(partnerDid, new DidDocCacheEntry(didJson, Instant.now()));
            return didJson;
        } catch (Exception e) {
            log.error("Failure while retrieving Did-Doc Request {}", partnerDid, e);
            return null;
        }
    }

    /**
     * Retrieves and inspects the membership credential for a given partner, using the provided token.
     *
     * @param accessTokenForPartnerCredentialService access token from the partner
     * @param partnerDid                             the id of the partner in question
     * @return List of presentations from partner credential service
     */
    private List<SignedJWT> fetchVerifiablePresentations(String accessTokenForPartnerCredentialService,
                                                         String partnerDid) {
        List<SignedJWT> signedCredentials = new ArrayList<>();
        try {
            // wrap partner token into self-signed token
            String selfSignedTokenForPartnerCredentialService = tokenProviderService.getWrappedToken(partnerDid, accessTokenForPartnerCredentialService);

            // call partner credential service using the self-signed token.
            String credentialServiceUrl = extractCredentialServiceUrlFromDidDocument(retrieveDidDocFromCacheOrFetch(partnerDid));
            credentialServiceUrl += "/presentations/query";
            String credServiceResponse = restClient
                    .post()
                    .uri(credentialServiceUrl)
                    .header("Authorization", "Bearer " + selfSignedTokenForPartnerCredentialService)
                    .header("Content-Type", "application/json")
                    .body(getPresentationQuery())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.info("Call to CredentialService Endpoint Status: {}", res.getStatusCode());
                    })
                    .body(String.class);
            JsonObject credServiceResponseJson = parse(credServiceResponse);
            log.info("Got Response from CredentialService \n{}", prettyPrint(credServiceResponseJson));

            // unwrap response to find the verifiable presentations and therein the verifiable credentials.
            if (credServiceResponseJson.getString("type").equals("PresentationResponseMessage")) {
                JsonValue presentationsValue = credServiceResponseJson.get("presentation");
                JsonArray presentationsArray;
                if (presentationsValue instanceof JsonArray array) {
                    presentationsArray = array;
                } else {
                    presentationsArray = Json.createArrayBuilder().add(presentationsValue).build();
                }
                for (var item : presentationsArray) {
                    if (item instanceof JsonString stringRepresentation) {
                        try {
                            SignedJWT signedPresentation = SignedJWT.parse(stringRepresentation.getString());
                            // presentation should be signed by the partner
                            if (verifyTokenSignature(signedPresentation, partnerDid)) {
                                JsonObject vpPayloadObject = parse(signedPresentation.getPayload().toString());
                                JsonObject vpObject = vpPayloadObject.getJsonObject("vp");
                                for (var vcItem : vpObject.getJsonArray("verifiableCredential")) {
                                    try {
                                        if (vcItem instanceof JsonString vcJsonString) {
                                            SignedJWT signedVerifiableCredential = SignedJWT.parse(vcJsonString.getString());
                                            String vcIssuer = signedVerifiableCredential.getJWTClaimsSet().getIssuer();
                                            // verifiable credential should be signed by a trusted issuer, it should not be
                                            // expired or revoked.
                                            if (TRUSTED_ISSUERS.contains(vcIssuer) && verifyTokenSignature(signedVerifiableCredential, vcIssuer) &&
                                                    checkNotExpiredAndNotRevoked(signedVerifiableCredential)) {
                                                signedCredentials.add(signedVerifiableCredential);
                                            } else {
                                                log.warn("Signature Validation failed for issuer {} and {}", vcIssuer, vcJsonString.getString());
                                            }
                                        } else {
                                            log.warn("Found unexpected content in verifiable credential {}", vcItem.toString());
                                        }

                                    } catch (Exception e) {
                                        log.warn("Failed to evaluate verifiable credential {}", vcItem.toString(), e);
                                    }
                                }
                            } else {
                                log.warn("Signature verification of signed presentation failed for {} and {}", partnerDid, item);
                            }
                        } catch (Exception e) {
                            log.warn("Found unexpected content in presentation {}", item);
                        }
                    } else {
                        log.warn("Found unexpected content in presentation {}", item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while fetching presentation", e);
        }
        return signedCredentials;
    }


    /**
     * This method checks, whether the given credential is neither expired nor revoked.
     *
     * @param signedVerifiableCredential the credential to inspect
     * @return true if not expired and not revoked, false otherwise
     */
    private boolean checkNotExpiredAndNotRevoked(SignedJWT signedVerifiableCredential) {
        try {
            Map<String, Object> vc = signedVerifiableCredential.getJWTClaimsSet().getJSONObjectClaim("vc");
            String expDate = vc.get("expirationDate").toString();
            if (Instant.now().isAfter(Instant.parse(expDate))) {
                log.warn("Vc is expired, rejecting");
                return false;
            }

            if (vc.get("credentialStatus") instanceof Map<?, ?> credStatusMap) {
                var purpose = credStatusMap.get("statusPurpose");
                if ("revocation".equals(purpose)) {
                    final int statusListIndex = Integer.parseInt(credStatusMap.get("statusListIndex").toString());
                    log.info("Status list index {}", statusListIndex);
                    if (credStatusMap.get("statusListCredential") instanceof String statusListUrlString) {
                        var cacheResult = revocationCache.get(statusListUrlString);
                        // prefer cached data
                        byte[] bytes = cacheResult != null ? cacheResult.bitstringList : null;
                        if (bytes == null) {
                            // if cache lookup yielded no result, fetch it from the URL specified by the issuer
                            var statusListResponse = restClient
                                    .get()
                                    .uri(statusListUrlString)
                                    .header("accept", "application/json") // an alternative would be "application/vc+jwt"
                                    .retrieve()
                                    .body(String.class);
                            var jsonResponse = parse(statusListResponse);
                            log.info("StatusList Response \n{}", prettyPrint(jsonResponse));
                            // unwrap response
                            var credSubjectVal = jsonResponse.get("credentialSubject");
                            JsonObject credSubject = null;
                            // can be either an object or an object within an array
                            if (credSubjectVal instanceof JsonArray jsonArray) {
                                for (var item : jsonArray) {
                                    if (item instanceof JsonObject jsonObject) {
                                        if ("revocation".equals(jsonObject.getString("statusPurpose"))) {
                                            credSubject = jsonObject;
                                            break;
                                        }
                                    }
                                }
                            } else if (credSubjectVal instanceof JsonObject jsonObject) {
                                if ("revocation".equals(jsonObject.getString("statusPurpose"))) {
                                    credSubject = jsonObject;
                                }
                            }
                            if (credSubject == null) {
                                log.warn("revocation credential not found");
                                return false;
                            }
                            final String finalEncodedList = credSubject.getString("encodedList");
                            final String urlEncodedList = finalEncodedList.startsWith("u") ? finalEncodedList.substring(1) : finalEncodedList;
                            // first try URL B64 decoder
                            try (GZIPInputStream gZip = new GZIPInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(urlEncodedList)));
                                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                gZip.transferTo(bos);
                                bytes = bos.toByteArray();
                                revocationCache.put(statusListUrlString, new RevocationCacheEntry(bytes, Instant.now()));
                            } catch (Exception e) {
                                // fallback: standard B64 decoder
                                try (GZIPInputStream gZip = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(finalEncodedList)));
                                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                    gZip.transferTo(bos);
                                    bytes = bos.toByteArray();
                                    revocationCache.put(statusListUrlString, new RevocationCacheEntry(bytes, Instant.now()));
                                }
                            }

                        }
                        // Non-Revocation is verified, if value at statusListIndex is '0'.
                        boolean result = statusListIndex < bytes.length && bytes[statusListIndex] == 0;
                        if (!result) {
                            log.warn("Could not verify that credential is not revoked, returning false:\n{}", signedVerifiableCredential);
                        }
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while evaluating revocation", e);
        }
        log.warn("Could not verify that credential is not revoked, returning false:\n{}", signedVerifiableCredential);
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
        presentationQuery.add("@context", presentationRequestContext);
        presentationQuery.add("type", "PresentationQueryMessage");
        var scopesArray = Json.createArrayBuilder();
        for (var scope : tokenProviderService.getPreparedScope().split(" ")) {
            scopesArray.add(scope);
        }
        presentationQuery.add("scope", scopesArray);
        return presentationQuery.build().toString();
    }


    /**
     * In order to keep the memory usage under control by getting rid of outdated data, the caches will be purged after
     * a certain timespan (currently: 1 day).
     */
    void doCleanups() {
        cacheLock.writeLock().lock();
        try {
            log.info("Doing scheduled cache cleanup (configured interval is {} minutes)", cacheUpdateInterval.toMinutes());
            cleanUpSeenJtis();
            cleanUpCache(revocationCache);
            cleanUpCache(didDocumentsCache);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private void cleanUpCache(Map<String, ? extends CacheEntryWithUpdatedInstant> cache) {
        Instant now = Instant.now();
        HashSet<String> expiredEntryKeys = new HashSet<>();
        for (var entry : cache.entrySet()) {
            if (entry.getValue().lastUpdated().plus(cacheUpdateInterval).isBefore(now)) {
                expiredEntryKeys.add(entry.getKey());
            }
        }
        expiredEntryKeys.forEach(cache::remove);
    }

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
     * This method receives the verifiable presentations and is expected to check the
     * payload objects within them. All entries in the verifiablePresentations list are
     * guaranteed to have a signature from a trusted issuer.
     * <p>
     * The method is expected to return a map. In most cases the findings in the presentation
     * should be laid down in a (potentially comma-separated) String under the "credentials" key (see
     * {@link org.factoryx.library.connector.embedded.provider.interfaces.DspTokenValidationService.ReservedKeys}
     * <p>
     * The "partnerId" should not be used (it will be overwritten anyway by the calling method in the super-class).
     * <p>
     * Other key-value pairs may be inserted as required, they can potentially be read in the embedding project.
     * <p>
     * A very simple example implementation can be found in the
     * {@link org.factoryx.library.connector.embedded.provider.service.dsp_validation.fxvalidation_v0_1.FXv0_1_ValidationService}
     * class.
     *
     * @param verifiablePresentations the verifiable presentations from the partners credential service
     * @param partnerDid              the DID-id of the partner
     * @return a map derived from the findings in the payloads (must not be null)
     */
    @Nonnull
    abstract protected Map<String, String> evaluateVerifiablePresentations(List<SignedJWT> verifiablePresentations, String partnerDid);
}
