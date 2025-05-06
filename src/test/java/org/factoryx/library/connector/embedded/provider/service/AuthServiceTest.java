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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;

import com.nimbusds.jwt.JWTClaimsSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuthServiceTest {

    @Mock
    private EnvService envService;
    private AuthorizationService authService;
    private static final String CONTRACT_ID = "57e5f3ac-c1ef-4361-89c9-71c51c18f089";
    private static final String DATA_ADDRESS = "http://localhost:8080/path/to/data-asset";
    private static final String PARTNER_ID = "partner-123";
    private static final String INVALID_PARTNER_ID = "partner-456";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(envService.getSingleAssetReadOnlyDataAccessIssuer()).thenReturn("test-issuer");
        authService = new AuthorizationService(envService);
    }

    /*
     * tests for access token creation and validation
     */
    @Test
    void testIssueDataAccessToken() {
        String token = authService.issueDataAccessToken(CONTRACT_ID, DATA_ADDRESS);
        assertNotNull(token, "Token should not be null");
    }

    @Test
    void testValidateToken_validToken() {
        String token = authService.issueDataAccessToken(CONTRACT_ID, DATA_ADDRESS);
        assertTrue(authService.validateToken(token), "Token should be valid");
    }

    @Test
    void testExtractAllClaims() {
        String token = authService.issueDataAccessToken(CONTRACT_ID, DATA_ADDRESS);
        JWTClaimsSet claims = authService.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("test-issuer", claims.getIssuer(), "Issuer should match");
        assertEquals(CONTRACT_ID, claims.getClaim(AuthorizationService.CONTRACT_ID), "Contract ID should match");
        assertEquals(DATA_ADDRESS, claims.getClaim(AuthorizationService.DATA_ADDRESS), "Data address should match");
    }

    @Test
    void testIncorrectTokenValidation() {

        String token = authService.issueDataAccessToken(CONTRACT_ID, DATA_ADDRESS);
        // insert unexpected character
        final int charIndex = 40;
        char differentChar = token.charAt(charIndex) == 'a' ? 'b' : 'a';
        StringBuilder temp = new StringBuilder(token);
        temp.setCharAt(charIndex, differentChar);
        String sabotagedToken = temp.toString();

        // Act
        boolean validationResult = authService.validateToken(sabotagedToken);

        // Assert
        assertFalse(validationResult, "Token validation should fail for incorrect token");
    }

    /*
     * tests for refresh token creation and validation
     */
    @Test
    void testIssueRefreshToken() throws Exception {
        String accessToken = authService.issueDataAccessToken(CONTRACT_ID, DATA_ADDRESS);
        String refreshToken = authService.issueRefreshToken(accessToken, PARTNER_ID);
        assertNotNull(refreshToken, "Refresh token should not be null");

        JWTClaimsSet refreshTokenClaims = authService.extractAllClaims(refreshToken);
        assertEquals("test-issuer", refreshTokenClaims.getIssuer(), "Refresh issuer should match");
        assertEquals(PARTNER_ID, refreshTokenClaims.getSubject(), "Subject should be partnerId");
        assertEquals(accessToken, refreshTokenClaims.getStringClaim(AuthorizationService.TOKEN), "Access token should match");
    }

    @Test
    void testRefreshTokenValidForMatchingPartner() {
        String accessToken = authService.issueDataAccessToken(CONTRACT_ID, DATA_ADDRESS);
        String refreshToken = authService.issueRefreshToken(accessToken, PARTNER_ID);

        assertTrue(authService.validateToken(refreshToken), "Refresh token should be valid (signature and exp)");
    }

    @Test
    void testRefreshTokenInvalidForDifferentPartner() throws Exception {
        String accessToken = authService.issueDataAccessToken(CONTRACT_ID, DATA_ADDRESS);
        String refreshToken = authService.issueRefreshToken(accessToken, PARTNER_ID);

        JWTClaimsSet refreshTokenClaims = authService.extractAllClaims(refreshToken);
        assertNotEquals(INVALID_PARTNER_ID, refreshTokenClaims.getSubject(), "Refresh token should not be valid for different partnerId");
    }
}
