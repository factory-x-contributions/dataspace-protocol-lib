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

package org.factoryx.library.connector.embedded.provider.interfaces;

import java.util.List;
import java.util.UUID;

/**
 * A class implementing this interface is expected to provide read-access to
 * the library on single DataAssets and a complete list of all available DataAssets.
 */
public interface DataAssetManagementService {

    /**
     * Retrieve a single DataAsset entity with the given id
     *
     * @param id The UUID that specifies a DataAsset
     * @return the DataAsset entity
     */
    DataAsset getById(UUID id);

    /**
     * Retrieve a list of all available DataAssets
     *
     * @return the list of DataAssets
     */
    List<? extends DataAsset> getAll();
}
