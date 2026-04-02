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

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

/**
 *
 */
@Slf4j
public abstract class BaseTokenProviderService implements DspTokenProviderService {

    protected final RestClient restClient;
    protected final Environment environment;
    protected final String backendId;
    protected final String vaultRootToken;
    protected final String vaultSecretUrl;
    protected String initialSecret;
    protected final String stsUrl;
    protected final boolean addBearer;
    protected final String credentialScope;
    protected final String credentialTypeSetting;
    private String preparedScope;

    protected BaseTokenProviderService(RestClient restClient, Environment environment, EnvService envService) {
        this.restClient = restClient;
        this.environment = environment;
        this.backendId = envService.getBackendId();
        vaultRootToken = environment.getProperty("org.factoryx.library.dcpvalidation.vaultroottoken", "root");
        vaultSecretUrl = environment.getProperty("org.factoryx.library.dcpvalidation.vaultsecreturl", "http://provider-vault:8200/v1/secret/data/myVaultAlias");
        addBearer = environment.getProperty("org.factoryx.library.dcpvalidation.addbearer", Boolean.class, false);
        if (environment.getProperty("org.factoryx.library.dcpvalidation.initialsecret") != null) {
            initialSecret = environment.getProperty("org.factoryx.library.dcpvalidation.initialsecret");
        } else {
            if (vaultSecretUrl == null || vaultRootToken == null) {
                String configErrorMessage = "Please configure either 'org.factoryx.library.dcpvalidation.initialsecret' or " +
                        "'org.factoryx.library.dcpvalidation.vaultroottoken' and 'org.factoryx.library.dcpvalidation.vaultsecreturl'!";
                log.error(configErrorMessage);
                throw new IllegalArgumentException(configErrorMessage);
            }
        }
        stsUrl = environment.getProperty("org.factoryx.library.dcpvalidation.stsurl", "");
        credentialScope = environment.getProperty("org.factoryx.library.dcpvalidation.credentialscope", "org.eclipse.tractusx.vc.type");
        credentialTypeSetting = environment.getProperty("org.factoryx.library.dcpvalidation.credentials", "MembershipCredential");
    }

    @Override
    public String provideTokenForPartner(NegotiationRecord record) {
        return (addBearer ? "Bearer " : "") + provideTokenForPartner(record.getPartnerId());
    }

    @Override
    public String provideTokenForPartner(TransferRecord record) {
        return (addBearer ? "Bearer " : "") + provideTokenForPartner(record.getPartnerId());
    }

    protected abstract String provideTokenForPartner(String partnerId);

    public abstract String getWrappedToken(String partnerDid, String tokenFromPartner);

    public String getPreparedScope() {
        if (this.preparedScope == null) {
            var credentialTypes = credentialTypeSetting.replace(" ", "").strip().split(",");
            StringBuilder builder = new StringBuilder();
            for (String type : credentialTypes) {
                builder.append(credentialScope).append(":").append(type).append(":read ");
            }
            preparedScope = builder.toString().strip();
        }
        return preparedScope;
    }

}
