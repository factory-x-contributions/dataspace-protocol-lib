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

package org.factoryx.library.connector.embedded.provider.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a dataspace profile context, which describes how a connector exposes a specific
 * Dataspace Protocol version, including endpoint path, binding, authentication, and identifier configuration.
 *
 * @author tobias-urb
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataspaceProfileContext {
    private String id;
    private String version;
    private String path;
    private String binding;
    private String serviceId;
    private String identifierType;
    private Auth auth;
    private String vocabulary;
}
