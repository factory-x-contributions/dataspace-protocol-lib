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

package org.factoryx.library.connector.embedded.provider.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Slf4j
/**
 * This service allows creating a JWT for authorizing a DSP transfer as well
 * as methods for conveniently validating a token's signature and accessing its
 * claims.
 *
 * @author dalmasoud
 * @author eschrewe
 *
 */
public class AuthorizationService {

    private final long tokenValidityInMilliSeconds = 1000L * 60 * 5; // five minutes
    private final long refreshTokenValidityInMilliSeconds = 1000L * 60 * 30; // thirty minutes
    private final Duration keyRotationInterval = Duration.ofHours(1); // must be larger than token validity
    public final static String CONTRACT_ID = "cid";
    public final static String DATA_ADDRESS = "dad";
    public final static String TOKEN = "token";
    public final static String ASSET_ID = "assetId";
    private final EnvService envService;

    private JWSSigner signer;
    private LocalDateTime signerInitializedAt = LocalDateTime.now();

    private JWSVerifier verifier;
    private JWSVerifier previousVerifier;

    /**
     * Prevent race conditions during key rotation.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public AuthorizationService(EnvService envService) {
        this.envService = envService;
        try {
            String secretString = generateSecretKey();
            signer = new MACSigner(secretString);
            verifier = new MACVerifier(secretString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateSecretKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }


    public String issueWriteAccessToken(String contractId, UUID assetId) {
        rotateKeys();
        lock.readLock().lock();
        try {
            long now = System.currentTimeMillis();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(envService.getApiAssetWriteAccessIssuer())
                    .claim(CONTRACT_ID, contractId)
                    .claim(ASSET_ID, assetId.toString())
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + tokenValidityInMilliSeconds))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing JWT", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Generates a JWT token with the required claims.
     *
     * @param cid the contract ID
     * @param dad the data address
     * @return the generated JWT token
     */
    public String issueDataAccessToken(String cid, String dad) {
        rotateKeys();

        lock.readLock().lock();
        try {
            long now = System.currentTimeMillis();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(envService.getSingleAssetReadOnlyDataAccessIssuer())
                    .claim(CONTRACT_ID, cid)
                    .claim(DATA_ADDRESS, dad)
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + tokenValidityInMilliSeconds))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing JWT", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Validates the JWT token's signature and expiration.
     *
     * @param token the JWT token to validate
     * @return true if the token has valid signature and is not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            lock.readLock().lock();
            boolean signatureValid = signedJWT.verify(verifier) || signedJWT.verify(previousVerifier);
            long leeway = 5000; // five seconds
            boolean notExpired = signedJWT.getJWTClaimsSet().getExpirationTime().getTime() >= System.currentTimeMillis() - leeway;
            return signatureValid && notExpired;
        } catch (Exception e) {
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token the JWT token
     * @return the claims extracted from the token
     */
    public JWTClaimsSet extractAllClaims(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Issues a refresh token for the given client ID.
     *
     * @param accessToken the access token associated with the refresh token
     * @param partnerId   the ID of the partner requesting the refresh token
     * @return the refresh token
     */
    public String issueRefreshToken(String accessToken, String partnerId) {
        rotateKeys();
        lock.readLock().lock();
        try {
            long now = System.currentTimeMillis();

            String issuerId = envService.getSingleAssetReadOnlyDataAccessIssuer();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuerId)
                    .subject(partnerId)
                    .claim(TOKEN, accessToken)
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + refreshTokenValidityInMilliSeconds))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing refresh token", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Rotates the keys used for signing and verifying JWT tokens if the key
     * rotation interval has passed.
     */
    private void rotateKeys() {
        lock.writeLock().lock();
        try {
            try {
                if (LocalDateTime.now().isAfter(signerInitializedAt.plus(keyRotationInterval))) {
                    String secretString = generateSecretKey();
                    signer = new MACSigner(secretString);
                    previousVerifier = verifier;
                    verifier = new MACVerifier(secretString);
                    signerInitializedAt = LocalDateTime.now();
                }
            } catch (JOSEException e) {
                throw new RuntimeException("Error rotating keys", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
