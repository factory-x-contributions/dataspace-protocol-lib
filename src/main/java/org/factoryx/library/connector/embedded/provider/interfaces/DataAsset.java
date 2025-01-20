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

import java.util.Map;
import java.util.UUID;

/**
 * This interface should be implemented by a class which represents (or wraps)
 * the data objects that you want to make available in the dataspace.
 */
public interface DataAsset {

    /**
     * A unique identifier for a DataAsset
     */
    UUID getId();

    /**
     * A set of key-value pairs that describe the asset. These will
     * be published in the DSP catalog for external partners to see.
     */
    Map<String, String> getProperties();

    /**
     *  This indicates the Http content type of the DTO's that you want to provide.
     *  Typically, this can be "application/json". But depending on your use-case,
     *  it might for example also be some specialised binary encoding, in which case
     *  something like "application/octet-stream" might be more appropriate.
     *
     */
    String getContentType();

    /**
     * This method provides the DTO representation of the DataAsset.
     * Since this library does not make any assumptions on which data format
     * you want to provide, the return type is defined as byte array.
     *
     * If, for example, you want to send JSON-based DTO's, you should be able to simply
     * use ".toString().getBytes()" in order to meet the required return type.
     *
     */
    byte[] getDtoRepresentation();

}
