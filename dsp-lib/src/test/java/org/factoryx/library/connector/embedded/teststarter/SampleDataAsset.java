/*
 * Copyright (c) 2025. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.factoryx.library.connector.embedded.teststarter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAsset;

import java.util.Map;
import java.util.UUID;

public class SampleDataAsset implements DataAsset {

    public static final String CATALOG_ASSET_ID = "207ed5a4-2eae-47af-bcb1-9202280d2700";
    public static final String NEGOTIATION_ASSET_ID = "207ed5a4-2eae-47af-bcb1-9202280d2701";

    private final static ObjectMapper MAPPER = new ObjectMapper();

    private UUID id;

    private final String fieldA = "fieldA" + UUID.randomUUID();

    private final String fieldB = "fieldB" + UUID.randomUUID();

    public SampleDataAsset(UUID id) {
        this.id = id;
    }

    public SampleDataAsset() {
        this(UUID.randomUUID());
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.of("type", "SampleDataAsset", "hasFieldA", "true", "hasFieldB", "true");
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public byte[] getDtoRepresentation() {
        try {
            return ("{" +
                    "\"id\": \"" + id + "\"," +
                    "\"fieldA\": \"" + fieldA + "\"," +
                    "\"fieldB\": \"" + fieldB + "\"" +
                    "}").getBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
