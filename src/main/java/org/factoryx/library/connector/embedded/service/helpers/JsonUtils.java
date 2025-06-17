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

package org.factoryx.library.connector.embedded.service.helpers;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * This class contains several useful static methods and values for handling JSON and JSON-LD.
 *
 * @author eschrewe
 * @author dalmasoud
 *
 */
public class JsonUtils {

    // Namespace constants:
    public static final String DSPACE_NAMESPACE = "https://w3id.org/dspace/v0.8/";
    public static final String ODRL_NAMESPACE = "http://www.w3.org/ns/odrl/2/";

    // Internal constants:
    private static final JsonWriterFactory WRITER_FACTORY = Json
            .createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));

    /**
     * Contains a template @context value (the same that the EDC Messages currently
     * contain).
     */
    public static final JsonObject FULL_CONTEXT = Json.createObjectBuilder()
            .add("@vocab", "https://w3id.org/edc/v0.0.1/ns/")
            .add("edc", "https://w3id.org/edc/v0.0.1/ns/")
            .add("dcat", "http://www.w3.org/ns/dcat#")
            .add("dct", "http://purl.org/dc/terms/")
            .add("odrl", ODRL_NAMESPACE)
            .add("dspace", DSPACE_NAMESPACE)
            .build();

    /**
     * Expands a compact JSON-LD object
     *
     * @param jsonObject - the compact JSON-LD
     * @return - the expanded version of that object
     */
    public static JsonObject expand(JsonObject jsonObject) {
        try {
            JsonDocument jsonDocument = JsonDocument.of(jsonObject);
            JsonArray array = JsonLd.expand(jsonDocument).get();
            return array.getJsonObject(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Parses a String that contains a JSON representation to a
     * JsonObject
     *
     * @param jsonString - the String representation of a JSON object
     * @return - a JsonObject
     */
    public static JsonObject parse(String jsonString) {
        return Json.createReader(new StringReader(jsonString)).readObject();
    }

    /**
     * Parses a String that contains a JSON representation to a
     * JsonArray
     *
     * @param jsonString - the String representation of a JSON array
     * @return - a JsonArray
     */
    public static JsonArray parseArray(String jsonString) {
        return Json.createReader(new StringReader(jsonString)).readArray();
    }

    /**
     * Parses a String that contains a JSON-LD representation
     * and expands it
     *
     * @param jsonString - the String representation of a JSON-LD object
     * @return - a JsonObject
     */
    public static JsonObject parseAndExpand(String jsonString) {
        return expand(parse(jsonString));
    }

    /**
     * Returns a prettified String representation of the JsonObject
     *
     * @param jsonObject - a JsonObject
     * @return - a prettified String representation of the JsonObject
     */
    public static String prettyPrint(JsonObject jsonObject) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        JsonWriter writer = WRITER_FACTORY.createWriter(bos);
        writer.write(jsonObject);
        writer.close();
        return bos.toString();
    }

    /**
     * Returns a prettified String representation
     *
     * @param jsonString - a String containing a representation of a JsonObject
     * @return - a prettified String representation
     */
    public static String prettyPrint(String jsonString) {
        return prettyPrint(parse(jsonString));
    }

    /**
     * Creates a JSON-LD response object for an error
     *
     * @param providerPid - the PID of the provider
     * @param consumerPid - the PID of the consumer
     * @param type        - the type of the error (e.g. "TransferError" or
     *                    "ContractNegotiationError")
     * @param reasons     - a list of reasons for the error
     * @return - a JSON-LD response object
     */
    public static byte[] createErrorResponse(String providerPid, String consumerPid, String type,
                                             List<String> reasons) {
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                .add("@context", FULL_CONTEXT)
                .add("@type", "dspace:" + type)
                .add("dspace:providerPid", providerPid)
                .add("dspace:consumerPid", consumerPid)
                // TODO: Examine possible error codes
                .add("dspace:code", "400");

        if (reasons != null && !reasons.isEmpty()) {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (String message : reasons) {
                JsonObject reason = Json.createObjectBuilder()
                        .add("@value", message)
                        .add("@language", "en")
                        .build();
                arrayBuilder.add(reason);
            }
            responseBuilder.add("dspace:reason", arrayBuilder.build());
        }

        return responseBuilder.build().toString().getBytes(StandardCharsets.UTF_8);
    }

}
