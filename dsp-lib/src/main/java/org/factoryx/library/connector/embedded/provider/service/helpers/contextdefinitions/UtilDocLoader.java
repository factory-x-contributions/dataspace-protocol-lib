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

package org.factoryx.library.connector.embedded.provider.service.helpers.contextdefinitions;

import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.parse;

public class UtilDocLoader implements DocumentLoader {
    static final Logger log = LoggerFactory.getLogger(UtilDocLoader.class);
    private final Map<URI, JsonDocument> CACHE = new HashMap<>();

    public UtilDocLoader() {
        CACHE.put(URI.create("https://w3id.org/dspace/2025/1/context.jsonld"), CacheProvider.getDSP2025Context());
        CACHE.put(URI.create("https://w3id.org/dspace/2025/1/odrl-profile.jsonld"), CacheProvider.getDSP2025OdrlProfile());
        CACHE.put(URI.create("https://w3id.org/edc/dspace/v0.0.1"), CacheProvider.getEDCContext());
    }

    @Override
    public Document loadDocument(URI uri, DocumentLoaderOptions options) {
        return CACHE.computeIfAbsent(uri, any -> fetchDocument(uri));
    }

    private JsonDocument fetchDocument(URI uri)  {
        try {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                try (var stream = new BufferedInputStream(connection.getInputStream())) {
                    String stringData = new String(stream.readAllBytes());
                    log.info("Loaded document from {}: \n{}", uri, stringData);
                    return JsonDocument.of(parse(stringData));
                }
            }
            log.error("Failed to load document from {}: \n{}", uri, connection.getResponseMessage());
        } catch (Exception e){
            log.error(e.getMessage());
        }
        return null;
    }
}
