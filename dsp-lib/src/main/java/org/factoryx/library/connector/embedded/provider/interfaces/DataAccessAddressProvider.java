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

/**
 * A class implementing this interface is expected to provide a customized
 * method that generates a URL for the chosen alternative to the built-in
 * Data-Access-Controller.
 *
 */
public interface DataAccessAddressProvider {

    /**
     * Create a URL for your alternative Data-Access-Controller,
     * which may or may not be specific for the given asset.
     *
     * @param dataAsset the data-asset in question
     * @return the Edr endpoint
     */
    String getAddressForDataAsset(DataAsset dataAsset);
}
