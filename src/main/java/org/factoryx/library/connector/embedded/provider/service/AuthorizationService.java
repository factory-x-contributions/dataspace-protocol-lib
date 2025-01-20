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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
@Slf4j
public class AuthorizationService {

    /**
     * Secret key for signing and verifying JWTs
     */
    private final SecretKey secretKey;
    private final long tokenValidityInMilliSeconds = 1000L * 60 * 5; // five minutes
    public final static String CONTRACT_ID = "cid";
    public final static String DATA_ADDRESS = "dad";
    private final EnvService envService;

    /**
     * In-memory store for revoked tokens.
     */
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();


    public AuthorizationService(EnvService envService) {
        this.envService = envService;
        String secretString = generateSecretKey();
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    private String generateSecretKey() {
        StringBuilder sb = new StringBuilder();
        ArrayList<Character> chars = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            chars.add(c);
            chars.add((char) (c + 32));
        }
        for (char c = '0'; c <= '9'; c++) {
            chars.add(c);
        }
        Random random = new SecureRandom();
        for (int i = 0; i < 48; i++) {
            sb.append(chars.get(random.nextInt(chars.size())));
        }
        return sb.toString();
    }

    /**
     * Generates a JWT token with the required claims.
     *
     * @param cid    the contract ID
     * @param dad    the data address
     * @return the generated JWT token
     */
    public String issueDataAccessToken(String cid, String dad) {
        return Jwts.builder()
                .claim(CONTRACT_ID, cid)
                .claim(DATA_ADDRESS, dad)
                .issuer(envService.getSingleAssetReadOnlyDataAccessIssuer())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenValidityInMilliSeconds))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

    }

    /**
     * Validates the JWT token.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (isTokenRevoked(token)) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            String cid = extractCid(token);
            String dad = extractDad(token);
            String issuer = claims.getIssuer();
            return (cid != null && dad != null && issuer != null && !isTokenExpired(claims));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Revokes a JWT token.
     *
     * @param token the JWT token to revoke
     */
    public void revokeToken(String token) {
        revokedTokens.add(token);
    }

    /**
     * Checks if a JWT token is revoked.
     *
     * @param token the JWT token to check
     * @return true if the token is revoked, false otherwise
     */
    private boolean isTokenRevoked(String token) {
        return revokedTokens.contains(token);
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token the JWT token
     * @return the claims extracted from the token
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts a specific claim from the JWT token.
     *
     * @param <T>            the type of the claim
     * @param token          the JWT token
     * @param claimsResolver the function to resolve the claim
     * @return the extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Checks if the JWT token is expired.
     *
     * @param claims the claims extracted from the token
     * @return true if the token is expired, false otherwise
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    /**
     * Extracts the contract ID (cid) from the JWT token.
     *
     * @param token the JWT token
     * @return the contract ID
     */
    public String extractCid(String token) {
        return extractClaim(token, claims -> claims.get(CONTRACT_ID, String.class));
    }

    /**
     * Extracts the data address (dad) from the JWT token.
     *
     * @param token the JWT token
     * @return the data address
     */
    public String extractDad(String token) {
        return extractClaim(token, claims -> claims.get(DATA_ADDRESS, String.class));
    }
}
