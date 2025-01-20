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

package org.factoryx.library.connector.embedded.provider.service.helpers;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
/**
 * This service provides access to property defined variables.
 *
 * @author eschrewe
 *
 */
public class EnvService {

    @Value("${org.factoryx.library.hostname:localhost}")
    private String hostName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${org.factoryx.library.id:provider}")
    @Getter
    private String backendId;

    @Value("${org.factoryx.library.dspapiprefix:/dsp}")
    @Getter
    private String dspApiPrefix;

    @Value("${org.factoryx.library.usetls:false}")
    private boolean useTls;

    @Value("${org.factoryx.library.usebuiltindataccess}")
    private boolean useBuiltInDataAccess;

    @Value("${org.factoryx.library.usebuiltindataccess:localhost}")
    private String alternativeDataAccessUrlPrefix;

    private String getURLPrefix(){
        return useTls ? "https://" : "http://";
    }

    public String getOwnDspUrl() {
        return getURLPrefix() + hostName + ":" + serverPort + dspApiPrefix;
    }

    public String getDatasetUrl(UUID datasetId) {
        if (useBuiltInDataAccess) {
            return getURLPrefix() + hostName + ":" + serverPort + dspApiPrefix + "/data-access/" + datasetId;
        } else {
            return alternativeDataAccessUrlPrefix + datasetId;
        }

    }

    /**
     * Generates the issuer claim value we expect for read-only transfer tokens
     *
     * @return the issuer-String
     */
    public String getSingleAssetReadOnlyDataAccessIssuer() {
        return backendId + "/singleAssetReadOnlyDataAccessIssuer";
    }
}
